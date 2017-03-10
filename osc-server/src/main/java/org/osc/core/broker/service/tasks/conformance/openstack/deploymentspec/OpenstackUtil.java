/*******************************************************************************
 * Copyright (c) Intel Corporation
 * Copyright (c) 2017
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.osc.core.broker.service.tasks.conformance.openstack.deploymentspec;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.jclouds.openstack.neutron.v2.domain.Port;
import org.jclouds.openstack.nova.v2_0.domain.InterfaceAttachment;
import org.jclouds.openstack.nova.v2_0.domain.PortState;
import org.jclouds.openstack.nova.v2_0.domain.Server;
import org.jclouds.openstack.nova.v2_0.domain.Server.Status;
import org.jclouds.openstack.nova.v2_0.domain.ServerExtendedAttributes;
import org.jclouds.openstack.nova.v2_0.domain.regionscoped.AvailabilityZoneDetails;
import org.osc.core.broker.job.Job;
import org.osc.core.broker.job.Job.JobCompletionListener;
import org.osc.core.broker.job.JobEngine;
import org.osc.core.broker.job.Task;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.events.SystemFailureType;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.broker.model.entities.virtualization.openstack.VM;
import org.osc.core.broker.model.entities.virtualization.openstack.VMPort;
import org.osc.core.broker.model.plugin.sdncontroller.SdnControllerApiFactory;
import org.osc.core.broker.rest.client.openstack.jcloud.Endpoint;
import org.osc.core.broker.rest.client.openstack.jcloud.HostAvailabilityZoneMapping;
import org.osc.core.broker.rest.client.openstack.jcloud.JCloudNeutron;
import org.osc.core.broker.rest.client.openstack.jcloud.JCloudNova;
import org.osc.core.broker.service.ConformService;
import org.osc.core.broker.service.alert.AlertGenerator;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.exceptions.VmidcException;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.persistence.SecurityGroupEntityMgr;
import org.osc.core.broker.service.persistence.VMEntityManager;
import org.osc.core.util.encryption.EncryptionException;
import org.osc.sdk.controller.element.NetworkElement;

public class OpenstackUtil {

    private static final Logger log = Logger.getLogger(OpenstackUtil.class);

    private static final int SLEEP_DISCOVERY_RETRIES = 10 * 1000; // 10 seconds
    private static final int MAX_DISCOVERY_RETRIES = 40;

    /**
     * Extract domainId for the given list of protected ports, which all belong to the same domain
     * @param tenantId
     * @param tenantName
     * @param vc
     * @param protectedPorts
     * @return
     * @throws IOException
     */
    public static String extractDomainId(String tenantId, String tenantName, VirtualizationConnector vc,
            List<NetworkElement> protectedPorts) throws IOException, EncryptionException {
        String domainId=null;
        Port port = null;
        try (
                JCloudNeutron neutron = new JCloudNeutron(new Endpoint(vc, tenantName));
                JCloudNova nova = new JCloudNova(new Endpoint(vc, tenantName))) {
            Set<String> regions = nova.listRegions();
            outerloop:
                for (String region : regions) {
                    for (NetworkElement elem : protectedPorts) {
                        port = neutron.getPortById(region, elem.getElementId());
                        if (port != null) {
                            domainId = neutron.getNetworkPortRouterDeviceId(tenantId, region, port);
                            if (domainId !=null) {
                                break outerloop;
                            }
                        }
                    }
                }
        }
        return domainId;
    }

    /**
     * Waits until the VM state becomes active. If the VM enters a terminal state, throws a VmidcException
     *
     */
    public static void ensureVmActive(VirtualizationConnector vc, String tenant, String region, String vmId)
            throws Exception {

        try (JCloudNova nova = new JCloudNova(new Endpoint(vc, tenant))) {
            Server server = null;
            int i = MAX_DISCOVERY_RETRIES;
            while (i > 0) {
                server = nova.getServer(region, vmId);
                if (server == null) {
                    throw new VmidcException("VM with id: '" + vmId + "' does not exist");
                }
                if (server.getStatus() == Status.ACTIVE) {
                    break;
                } else if (isVmStatusTerminal(server.getStatus())) {
                    throw new VmidcException("VM is in bad state (" + server.getStatus() + ")");
                }

                log.info("Retry VM discovery (" + i + "/" + MAX_DISCOVERY_RETRIES + ") of VM '" + vmId + "' Status: "
                        + server.getStatus());
                Thread.sleep(SLEEP_DISCOVERY_RETRIES);
                i--;
            }
            if (server.getStatus() != Status.ACTIVE) {
                throw new VmidcException("VM with id: '" + vmId + "' is not in ready state (" + server.getStatus()
                + ")");
            }

            i = MAX_DISCOVERY_RETRIES;
            int activePorts = 0;
            while (i > 0) {
                List<InterfaceAttachment> interfaces = nova.getVmAttachedNetworks(region, vmId);

                activePorts = 0;
                for (InterfaceAttachment infs : interfaces) {
                    if (infs.getPortState().equals(PortState.ACTIVE) && infs.getFixedIps() != null) {
                        activePorts += infs.getFixedIps().size();
                    }
                }
                if (activePorts >= 2) {
                    log.info("VM network discovery (interfaces: " + interfaces + ")");
                    break;
                }
                log.info("Retry VM network discovery (" + i + "/" + MAX_DISCOVERY_RETRIES + ") of VM '"
                        + server.getName() + "' (interfaces: " + interfaces + ")");
                Thread.sleep(SLEEP_DISCOVERY_RETRIES);
                i--;
            }
            if (activePorts < 2) {
                throw new VmidcException("VM '" + server.getName() + "' network is not ready.");
            }
        }
    }

    /**
     * Finds a DAI/SVA given the region, tenant, host for the given virtual system.
     * 1) We first attempt to use exclusive Deployment spec for the specific tenant specified by the tenant id.
     * If found, we ensure there are DAI(s) on the host. If so, pick one based on port assignment load balancing.
     * 2) If above not found, we try to find a shared deployment specs which has a DAI on the host with list usage.
     * If found, we try finding the least loaded (based on port assignment).
     * 3) If still not found, we'll attempt to find an instance off-box (if SDN controller redirection is support).
     * We'll try to locate DAIs running on hosts deployed on the same availability zone as requested host.
     * If found, we try finding the least loaded (based on port assignment).
     *
     *
     * @param session
     *            session
     * @param region
     *            the region to search for DAI
     * @param tenantId
     *            the tenant ID to find a dedicated deployment spec to latch onto. If empty string is passed then
     *            we will find a shared deployment spec. Cannot pass in null
     * @param host
     *            the host to find the DAI on
     * @param vs
     *            the virtual system(service) appliance to look for
     *
     * @return a deployed SVA/DAI
     *
     * @throws VmidcBrokerValidationException
     *             if there are no valid DAI/SVA's available with the provided criteria.
     */
    public static DistributedApplianceInstance findDeployedDAI(Session session, String region, String tenantId,
            String host, VirtualSystem vs) throws Exception {

        DeploymentSpec selectedDs = null;

        ArrayList<DeploymentSpec> exclusiveDsList = new ArrayList<DeploymentSpec>();
        ArrayList<DeploymentSpec> sharedDsList = new ArrayList<DeploymentSpec>();

        // Get all DSs that are uses the same region
        for (DeploymentSpec ds : vs.getDeploymentSpecs()) {
            // Examine only DS of same region
            if (ds.getRegion().equals(region)) {
                if (ds.isShared()) {
                    sharedDsList.add(ds);
                } else if (ds.getTenantId().equals(tenantId)) {
                    exclusiveDsList.add(ds);
                }
            }
        }

        DistributedApplianceInstance dai = null;
        if (exclusiveDsList.size() > 0) {
            // If we found an exclusive DS, there must be one and only one for that tenant
            selectedDs = exclusiveDsList.get(0);
            dai = findLeastLoadedDAI(selectedDs.getDistributedApplianceInstances(), host);
            if (dai == null) {
                // It is possible that the exclusive DS does not contain a deployment on the requested host.
                // Resort to shared DS.
                selectedDs = null;
            } else {
                return dai;
            }
        }

        if (selectedDs == null) {
            if (!sharedDsList.isEmpty() && host == null) {
                // for router protection we will provide first Shared DS we have...
                selectedDs = sharedDsList.get(0);
            } else {
                // If a exclusive DS not found, search for a shared deployment
                // specs (including dynamic which is shared by definition).
                selectedDs = findLeastLoadedDSWithHost(sharedDsList, host);
            }
        }

        if (selectedDs != null) {
            // We found a DS that includes a deployment(s) on the host.
            // It is possible there are more then one instance deployed on the same host so need load balance.
            // We'll choose the one least used, by total count of ports handled by a DIA.
            //
            // Later on we can add more sophisticated algorithm to leverage historic throughput and virtual
            // resource utilization.
            return findLeastLoadedDAI(selectedDs.getDistributedApplianceInstances(), host);

        } else {

            // No DS found that has deployments on that host. Our last resort is to try off-boxing
            if (!SdnControllerApiFactory.supportsOffboxRedirection(vs)) {
                // We did not find a DS with a host deployment, either exclusive or shared.
                // And, our SDN controller does not support off-box traffic redirection.
                throw new VmidcBrokerValidationException(
                        "No Distributed Appliance Deployment Specifications found covering host '" + host + "'");
            }

            // If we've reached this point, that means we did not find DAI on the requested host and our controller
            // supports off-box redirection.
            // Find a DAI on a different host that is in close proximity (same Availability-Zone as requested host).

            try (JCloudNova novaApi = new JCloudNova(new Endpoint(vs.getVirtualizationConnector()))){
                // First figure out current host/AZ map.
                List<AvailabilityZoneDetails> osAvailabilityZones = novaApi.getAvailabilityZonesDetail(region);
                HostAvailabilityZoneMapping hostAvailabilityZoneMap = JCloudNova.getMapping(osAvailabilityZones);

                DistributedApplianceInstance selectedDai = null;
                // Get AZ for host in question
                if (host != null) {
                    String az = hostAvailabilityZoneMap.getHostAvailibilityZone(host);

                    // Search for DAI in exclusive DSs of the same region and tenant
                    selectedDai = findLeastLoadedDAI(getDAIsByAZ(exclusiveDsList, hostAvailabilityZoneMap, az), null);

                    if (selectedDai == null) {
                        // No exclusive DAI found
                        selectedDai = findLeastLoadedDAI(getDAIsByAZ(sharedDsList, hostAvailabilityZoneMap, az), null);
                    }
                    if (selectedDai == null) {
                        throw new VmidcBrokerValidationException(
                                "No Appliance Instance found to handle traffic inspection within the same Availibility Zone ('"
                                        + az + "') as host '" + host + "'");
                    }
                }

                return selectedDai;

            }
        }
    }

    private static DeploymentSpec findLeastLoadedDSWithHost(List<DeploymentSpec> dsList, String host) throws Exception {
        if (dsList == null || dsList.isEmpty()) {
            return null;
        }
        // Choose DS/DDS that has a DAI on the requested host with the least amount of protected ports
        DistributedApplianceInstance selectedDai = findLeastLoadedDAI(getDAIsByHost(dsList, host), null);
        if (selectedDai != null) {
            return selectedDai.getDeploymentSpec();
        }

        return null;
    }

    private static DistributedApplianceInstance findLeastLoadedDAI(Collection<DistributedApplianceInstance> dais,
            String host) {

        DistributedApplianceInstance selectedDai = null;
        String normalizedHostName = null;
        if (host != null) {
            normalizedHostName = OpenstackUtil.normalizeHostName(host);
        }

        for (DistributedApplianceInstance dai : dais) {
            if (host != null && dai.getOsHostName().equals(host) || dai.getOsHostName().equals(normalizedHostName)) {
                if (selectedDai == null) {
                    selectedDai = dai;
                } else if (dai.getProtectedPorts().size() < selectedDai.getProtectedPorts().size()) {
                    selectedDai = dai;
                }
            } else {
                if (selectedDai == null) {
                    selectedDai = dai;
                } else if (dai.getProtectedPorts().size() < selectedDai.getProtectedPorts().size()) {
                    selectedDai = dai;
                }
            }
        }
        return selectedDai;
    }

    private static ArrayList<DistributedApplianceInstance> getDAIsByHost(Collection<DeploymentSpec> dsList, String host) {
        String normalizedHostName = OpenstackUtil.normalizeHostName(host);

        ArrayList<DistributedApplianceInstance> dais = new ArrayList<DistributedApplianceInstance>();
        for (DeploymentSpec ds : dsList) {
            // TODO: Future. Optimize by using DB query
            for (DistributedApplianceInstance dai : ds.getDistributedApplianceInstances()) {
                if (dai.getOsHostName().equals(host) || dai.getOsHostName().equals(normalizedHostName)) {
                    dais.add(dai);
                }
            }
        }
        return dais;
    }

    private static ArrayList<DistributedApplianceInstance> getDAIsByAZ(ArrayList<DeploymentSpec> dsList,
            HostAvailabilityZoneMapping hostAvailabilityZoneMap, String az) throws VmidcException {
        ArrayList<DistributedApplianceInstance> dais = new ArrayList<DistributedApplianceInstance>();
        for (DeploymentSpec ds : dsList) {
            for (DistributedApplianceInstance dai : ds.getDistributedApplianceInstances()) {
                // TODO: Future. Optimize by using DB query
                String daiAz = hostAvailabilityZoneMap.getHostAvailibilityZone(dai.getHostName());
                if (daiAz != null && daiAz.equals(az)) {
                    dais.add(dai);
                }
            }
        }
        return dais;
    }

    /**
     * Gives you the actual hostname and not the FQDN
     *
     * @param hostname
     * @return
     */
    public static String normalizeHostName(String hostname) {
        return hostname.split("\\.")[0];
    }

    private static boolean isVmStatusTerminal(Status status) {
        return status == Status.SUSPENDED || status == Status.PAUSED || status == Status.UNKNOWN
                || status == Status.ERROR;
    }

    public static String getHostAvailibilityZone(DeploymentSpec ds, String region, String hostname) throws Exception {
        try (JCloudNova novaApi = new JCloudNova(new Endpoint(ds))){
            List<AvailabilityZoneDetails> osAvailabilityZones = novaApi.getAvailabilityZonesDetail(region);
            HostAvailabilityZoneMapping hostAvailabilityZoneMap = JCloudNova.getMapping(osAvailabilityZones);
            return hostAvailabilityZoneMap.getHostAvailibilityZone(hostname);
        }
    }

    /**
     * For any security groups protected by the DAI, schedules security group sync jobs to be run at the end of the job
     * containing the task.
     *
     * @param dai
     *            the dai which is protecting the security groups
     * @param task
     *            the current task. This is used to determine the current job so we can schedule the SG sync at the
     *            end of the current job.
     */
    public static void scheduleSecurityGroupJobsRelatedToDai(Session session, DistributedApplianceInstance dai,
            Task task) {
        // Check if existing SG members
        Set<SecurityGroup> sgs = SecurityGroupEntityMgr.listByDai(session, dai);

        if (!sgs.isEmpty()) {
            Job job = JobEngine.getEngine().getJobByTask(task);
            for (SecurityGroup sg : sgs) {
                log.info("Scheduling SG job for sg: " + sg + " on behalf of DAI: " + dai);
                job.addListener(new StartSecurityGroupJobListener(sg));
            }
        }
    }

    private static final class StartSecurityGroupJobListener implements JobCompletionListener {
        private final Logger log = Logger.getLogger(StartSecurityGroupJobListener.class);

        private final SecurityGroup sg;

        private StartSecurityGroupJobListener(SecurityGroup sg) {
            this.sg = sg;
        }

        @Override
        public void completed(Job job) {
            try {
                ConformService.startSecurityGroupConformanceJob(this.sg);
            } catch (Exception e) {
                AlertGenerator.processSystemFailureEvent(SystemFailureType.SCHEDULER_FAILURE, new LockObjectReference(
                        this.sg), "Failed to submit a dependent Security Group sync job " + e.getMessage());
                this.log.error("Fail to submit a dependent SG sync job for sg: " + this.sg, e);
            }
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.sg);
        }

        @Override
        public boolean equals(Object obj) {
            return Objects.equals(this.sg, obj);
        }
    }

    /**
     * @param session
     *            Hibernate session object
     * @param region
     *            Region this entity belongs to
     * @param sg
     *            Security Group in context
     * @param osPort
     *            Open stack port
     * @param vmPort
     *            VM port
     * @throws IOException
     */
    public static void discoverVmForPort(Session session, String region, SecurityGroup sg, Port osPort, VMPort vmPort)
            throws IOException, EncryptionException {

        JCloudNova nova = null;
        try {
            nova = new JCloudNova(new Endpoint(sg.getVirtualizationConnector(), sg.getTenantName()));
            Server osVm = nova.getServer(region, osPort.getDeviceId());
            if (null == osVm) {
                EntityManager.delete(session, vmPort);
                //TODO sridhar handle stale VM delete ?
                return;

            }
            VM vm = VMEntityManager.findByOpenstackId(session, osPort.getDeviceId());
            if (vm == null) {
                vm = new VM(region, osPort.getDeviceId(), osVm.getName());
                EntityManager.create(session, vm);
            }
            vmPort.setVm(vm);
            // Update vm host if needed
            ServerExtendedAttributes serverExtendedAttributes = osVm.getExtendedAttributes().get();
            if (serverExtendedAttributes != null && serverExtendedAttributes.getHypervisorHostName() != null) {
                if (!serverExtendedAttributes.getHypervisorHostName().equals(vm.getHost())) {
                    vm.setHost(serverExtendedAttributes.getHypervisorHostName());
                    EntityManager.update(session, vm);
                }
            }
            EntityManager.update(session, vmPort);
        } finally {
            if (nova != null) {
                nova.close();
            }
        }
    }
}
