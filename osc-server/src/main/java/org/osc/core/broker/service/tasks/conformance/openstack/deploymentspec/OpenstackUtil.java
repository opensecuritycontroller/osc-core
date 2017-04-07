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
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
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
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMember;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMemberType;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.broker.model.entities.virtualization.openstack.VM;
import org.osc.core.broker.model.entities.virtualization.openstack.VMPort;
import org.osc.core.broker.model.plugin.sdncontroller.NetworkElementImpl;
import org.osc.core.broker.model.plugin.sdncontroller.SdnControllerApiFactory;
import org.osc.core.broker.rest.client.openstack.jcloud.Endpoint;
import org.osc.core.broker.rest.client.openstack.jcloud.HostAvailabilityZoneMapping;
import org.osc.core.broker.rest.client.openstack.jcloud.JCloudNeutron;
import org.osc.core.broker.rest.client.openstack.jcloud.JCloudNova;
import org.osc.core.broker.service.ConformService;
import org.osc.core.broker.service.alert.AlertGenerator;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.exceptions.VmidcException;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.persistence.SecurityGroupEntityMgr;
import org.osc.core.broker.service.persistence.VMEntityManager;
import org.osc.core.util.encryption.EncryptionException;
import org.osc.sdk.controller.DefaultNetworkPort;
import org.osc.sdk.controller.element.NetworkElement;

public class OpenstackUtil {

    private static final Logger LOG = Logger.getLogger(OpenstackUtil.class);

    private static final int SLEEP_DISCOVERY_RETRIES = 10 * 1000; // 10 seconds
    private static final int MAX_DISCOVERY_RETRIES = 40;

    // TODO Sridhar: pass in just the IDs of the protectedports and remove break outerloop
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

    public static VMPort getAnyProtectedPort(SecurityGroup sg) {
        for (SecurityGroupMember sgm : sg.getSecurityGroupMembers()) {
            // If SGM is marked for deletion, previous tasks should have removed the hooks and deleted the member from D.
            if (sgm.getType() == SecurityGroupMemberType.VM) {
                return sgm.getVm().getPorts().iterator().next();
            } else if (sgm.getType() == SecurityGroupMemberType.NETWORK) {
                return sgm.getNetwork().getPorts().iterator().next();
            } else if (sgm.getType() == SecurityGroupMemberType.SUBNET) {
                return sgm.getSubnet().getPorts().iterator().next();
            }
        }

        return null;
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

                LOG.info("Retry VM discovery (" + i + "/" + MAX_DISCOVERY_RETRIES + ") of VM '" + vmId + "' Status: "
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
                    LOG.info("VM network discovery (interfaces: " + interfaces + ")");
                    break;
                }
                LOG.info("Retry VM network discovery (" + i + "/" + MAX_DISCOVERY_RETRIES + ") of VM '"
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
     * @param session  the database session
     * @param region  the OpenStack region of the deployment
     * @param tenantId  the OpenStack tenant of the deployment.
     *            If empty string, the search will target shared deployment sepcs. This parameter cannot be null.
     * @param host the host of the DAI
     * @param vs  the virtual system of the deployment specs.
     * @return a deployed SVA/DAI
     *
     * @throws VmidcBrokerValidationException if there are no valid DAI/SVA's available with the provided criteria.
     */
    public static DistributedApplianceInstance findDeployedDAI(
            EntityManager em,
            VirtualSystem vs,
            SecurityGroup sg,
            String tenantId,
            String region,
            String domainId,
            String host) throws Exception {

        DeploymentSpec selectedDs = null;
        boolean supportsOffboxRedirection = SdnControllerApiFactory.supportsOffboxRedirection(vs);

        // Get all DSs that are uses the same region
        for (DeploymentSpec ds : vs.getDeploymentSpecs()) {
            // Examine only DS of same region
            if (ds.getRegion().equals(region) && ds.getTenantId().equals(tenantId)) {
                selectedDs = ds;
                // If we found an exclusive DS, there must be one and only one for that tenant
                break;
            }
        }

        DistributedApplianceInstance dai = null;

        if (selectedDs == null || selectedDs.getDistributedApplianceInstances().isEmpty()) {
            // We did not find a DS with a host deployment and
            // the SDN controller does not support off-box traffic redirection.
            throw new VmidcBrokerValidationException(String.format("No distributed appliance instance was found for the tenant %s and host %s.", tenantId, host));
        }

        if (!supportsOffboxRedirection) {
            return findLeastLoadedDAI(selectedDs.getDistributedApplianceInstances(), sg, host, domainId);
        } else {
            try  {
                dai = findLeastLoadedDAI(selectedDs.getDistributedApplianceInstances(), sg, host, domainId);
            } catch (Exception ex){
                if (host == null) {
                    // The host is not defined, availability zone is not applicable. Stop here.
                    throw ex;
                }

                LOG.warn("Finding a DAI returned an error, "
                        + "attempting to find an instance offbox as the SDN controller supports offbox redirection", ex);
            }

            if (dai != null) {
                return dai;
            }

            List<DistributedApplianceInstance> azDais = getDAIsByHostAZ(selectedDs, vs, region, host);
            return findLeastLoadedDAI(azDais, sg, null, domainId);
        }
    }

    public static List<NetworkElement> getPorts(SecurityGroupMember sgm) throws VmidcBrokerValidationException {

        Set<VMPort> ports;
        switch (sgm.getType()) {
        case VM:
            ports = sgm.getVm().getPorts();
            break;
        case NETWORK:
            ports = sgm.getNetwork().getPorts();
            break;
        case SUBNET:
            ports = sgm.getSubnet().getPorts();
            break;
        default:
            throw new VmidcBrokerValidationException("Region is not applicable for Members of type '" + sgm.getType() + "'");
        }

        return ports.stream()
                .map(NetworkElementImpl::new)
                .collect(Collectors.toList());
    }

    private static DistributedApplianceInstance findLeastLoadedDAI(
            Collection<DistributedApplianceInstance> dais,
            SecurityGroup sg,
            String host,
            String domainId) throws Exception {

        DistributedApplianceInstance selectedDai = null;

        dais = filterDAIsByDomain(dais, sg.getVirtualizationConnector(), sg.getVirtualizationConnector().getProviderAdminTenantName(), domainId);

        if (dais.isEmpty()) {
            throw new VmidcBrokerValidationException(String.format("No appliance instance found in a network assigned to the router/domain %s.", domainId));
        }

        dais = filterDAIsByHost(dais, host);

        if (dais.isEmpty()) {
            throw new VmidcBrokerValidationException(String.format("No appliance instance found for host %s.", host));
        }

        // If no DAI was yet assigned to a security group then return the least loaded one.
        for (DistributedApplianceInstance dai : dais) {
            if (selectedDai == null) {
                selectedDai = dai;
            } else if (dai.getProtectedPorts().size() < selectedDai.getProtectedPorts().size()) {
                selectedDai = dai;
            }
        }

        return selectedDai;
    }

    private static Collection<DistributedApplianceInstance> filterDAIsByDomain(
            Collection<DistributedApplianceInstance> dais,
            VirtualizationConnector vc,
            String tenantId,
            String domainId) throws Exception  {
        if (StringUtils.isBlank(domainId)) {
            // No domain identifier provided, nothing to filter.
            return dais;
        }

        return dais
                .stream()
                .filter(dai -> {
                    try {
                        return extractDomainId(tenantId, vc, dai).equals(domainId);
                    } catch (Exception ex) {
                        LOG.error(String.format("Failure while extracting the domain for DAI %s.", dai.getName(), ex));
                        return false;
                    }
                })
                .collect(Collectors.toList());

    }

    private static Collection<DistributedApplianceInstance> filterDAIsByHost(Collection<DistributedApplianceInstance> dais, String host) {
        if (StringUtils.isBlank(host)) {
            return dais;
        }

        String normalizedHostName = OpenstackUtil.normalizeHostName(host);

        return dais
                .stream()
                .filter(dai -> dai.getOsHostName().equals(host) || dai.getOsHostName().equals(normalizedHostName))
                .collect(Collectors.toList());
    }

    // TODO emanoel: We should revisit the signature and implementation of the existing extractDomainId instead.
    // I.e.: It should not take the entire network element since it just needs the id.
    // Not doing that now to avoid additional churn on the Nuage current coding efforts.
    private static String extractDomainId(String tenantId, VirtualizationConnector vc, DistributedApplianceInstance dai) throws IOException, EncryptionException {
        DefaultNetworkPort ingressPort = new DefaultNetworkPort(dai.getInspectionOsIngressPortId(), dai.getInspectionIngressMacAddress());
        return OpenstackUtil.extractDomainId(
                dai.getDeploymentSpec().getTenantId(),
                tenantId,
                vc,
                Arrays.asList(ingressPort));
    }

    private static ArrayList<DistributedApplianceInstance> getDAIsByHostAZ(DeploymentSpec ds, VirtualSystem vs, String region, String host) throws Exception {
        ArrayList<DistributedApplianceInstance> dais = new ArrayList<DistributedApplianceInstance>();

        try (JCloudNova novaApi = new JCloudNova(new Endpoint(vs.getVirtualizationConnector()))){
            // First figure out current host/AZ map.
            List<AvailabilityZoneDetails> osAvailabilityZones = novaApi.getAvailabilityZonesDetail(region);
            HostAvailabilityZoneMapping hostAvailabilityZoneMap = JCloudNova.getMapping(osAvailabilityZones);

            // Get AZ for host in question
            if (host != null) {
                String az = hostAvailabilityZoneMap.getHostAvailibilityZone(host);

                // Search for DAI in exclusive DSs of the same region and tenant
                for (DistributedApplianceInstance dai : ds.getDistributedApplianceInstances()) {
                    // TODO: Future. Optimize by using DB query
                    String daiAz = hostAvailabilityZoneMap.getHostAvailibilityZone(dai.getHostName());
                    if (daiAz != null && daiAz.equals(az)) {
                        dais.add(dai);
                    }
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
    public static void scheduleSecurityGroupJobsRelatedToDai(EntityManager em, DistributedApplianceInstance dai,
            Task task) {
        // Check if existing SG members
        Set<SecurityGroup> sgs = SecurityGroupEntityMgr.listByDai(em, dai);

        if (!sgs.isEmpty()) {
            Job job = JobEngine.getEngine().getJobByTask(task);
            for (SecurityGroup sg : sgs) {
                LOG.info("Scheduling SG job for sg: " + sg + " on behalf of DAI: " + dai);
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
    public static void discoverVmForPort(EntityManager em, String region, SecurityGroup sg, Port osPort, VMPort vmPort)
            throws IOException, EncryptionException {

        JCloudNova nova = null;
        try {
            nova = new JCloudNova(new Endpoint(sg.getVirtualizationConnector(), sg.getTenantName()));
            Server osVm = nova.getServer(region, osPort.getDeviceId());
            if (null == osVm) {
                OSCEntityManager.delete(em, vmPort);
                //TODO sridhar handle stale VM delete ?
                return;

            }
            VM vm = VMEntityManager.findByOpenstackId(em, osPort.getDeviceId());
            if (vm == null) {
                vm = new VM(region, osPort.getDeviceId(), osVm.getName());
                OSCEntityManager.create(em, vm);
            }
            vmPort.setVm(vm);
            // Update vm host if needed
            ServerExtendedAttributes serverExtendedAttributes = osVm.getExtendedAttributes().get();
            if (serverExtendedAttributes != null && serverExtendedAttributes.getHypervisorHostName() != null) {
                if (!serverExtendedAttributes.getHypervisorHostName().equals(vm.getHost())) {
                    vm.setHost(serverExtendedAttributes.getHypervisorHostName());
                    OSCEntityManager.update(em, vm);
                }
            }
            OSCEntityManager.update(em, vmPort);
        } finally {
            if (nova != null) {
                nova.close();
            }
        }
    }
}
