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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;

import org.osc.core.broker.job.Task;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.virtualization.openstack.AvailabilityZone;
import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.broker.model.entities.virtualization.openstack.Host;
import org.osc.core.broker.model.entities.virtualization.openstack.HostAggregate;
import org.osc.core.broker.rest.client.openstack.openstack4j.Endpoint;
import org.osc.core.broker.rest.client.openstack.openstack4j.HostAvailabilityZoneMapping;
import org.osc.core.broker.rest.client.openstack.openstack4j.HostAvailabilityZoneMapping.HostAzInfo;
import org.osc.core.broker.rest.client.openstack.openstack4j.Openstack4JNova;
import org.osc.core.broker.service.exceptions.VmidcException;
import org.osc.core.broker.service.persistence.DeploymentSpecEntityMgr;
import org.osc.core.broker.service.persistence.DistributedApplianceInstanceEntityMgr;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.tasks.FailedWithObjectInfoTask;
import org.osc.core.broker.service.tasks.TransactionalMetaTask;
import org.osc.core.broker.service.tasks.conformance.manager.MgrCheckDevicesMetaTask;
import org.osc.core.broker.service.tasks.conformance.openstack.DeleteOsSecurityGroupTask;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = DSUpdateOrDeleteMetaTask.class)
public class DSUpdateOrDeleteMetaTask extends TransactionalMetaTask {

    private static final Logger log = LoggerFactory.getLogger(DSUpdateOrDeleteMetaTask.class);

    @Reference
    MgrCheckDevicesMetaTask mgrCheckDevicesMetaTask;

    @Reference
    DeleteOsSecurityGroupTask deleteOSSecurityGroup;

    @Reference
    DeleteDSFromDbTask deleteDsFromDb;

    @Reference
    OsSvaCreateMetaTask osSvaCreateMetaTask;

    @Reference
    OsDAIConformanceCheckMetaTask osDAIConformanceCheckMetaTask;

    @Reference
    DeleteSvaServerAndDAIMetaTask deleteSvaServerAndDAIMetaTask;

    private Task firstCreatePGTask;

    private DeploymentSpec ds;
    private Endpoint endPoint;
    private TaskGraph tg;
    private Openstack4JNova novaApi;

    public DSUpdateOrDeleteMetaTask create(DeploymentSpec ds, Endpoint endPoint) {
        DSUpdateOrDeleteMetaTask task = new DSUpdateOrDeleteMetaTask();
        task.ds = ds;
        task.endPoint = endPoint;
        task.name = task.getName();

        task.osSvaCreateMetaTask = this.osSvaCreateMetaTask;
        task.osDAIConformanceCheckMetaTask = this.osDAIConformanceCheckMetaTask;
        task.mgrCheckDevicesMetaTask = this.mgrCheckDevicesMetaTask;
        task.deleteSvaServerAndDAIMetaTask = this.deleteSvaServerAndDAIMetaTask;
        task.deleteOSSecurityGroup = this.deleteOSSecurityGroup;
        task.deleteDsFromDb = this.deleteDsFromDb;
        task.dbConnectionManager = this.dbConnectionManager;
        task.txBroadcastUtil = this.txBroadcastUtil;

        return task;
    }

    DSUpdateOrDeleteMetaTask create(DeploymentSpec ds, Openstack4JNova novaApi) {
        DSUpdateOrDeleteMetaTask task = create(ds, (Endpoint) null);
        task.novaApi = novaApi;
        return task;
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {
        this.tg = new TaskGraph();
        OSCEntityManager<DeploymentSpec> emgr = new OSCEntityManager<DeploymentSpec>(DeploymentSpec.class, em, this.txBroadcastUtil);
        this.ds = emgr.findByPrimaryKey(this.ds.getId());
        VirtualSystem virtualSystem = this.ds.getVirtualSystem();
        if (this.ds.getMarkedForDeletion() || virtualSystem.getMarkedForDeletion()
                || virtualSystem.getDistributedAppliance().getMarkedForDeletion()) {
            log.info("DS " + this.ds.getName() + " marked for deletion, deleting DS");

            // No need to schedule dsClearPortGroupTask after these, since ds itself will be gone
            for (DistributedApplianceInstance dai : this.ds.getDistributedApplianceInstances()) {
                this.tg.addTask(this.deleteSvaServerAndDAIMetaTask.create(this.ds.getRegion(), dai));
            }

            if (this.ds.getOsSecurityGroupReference() != null) {
                this.tg.appendTask(this.deleteOSSecurityGroup.create(this.ds, this.ds.getOsSecurityGroupReference()));
            }
            this.tg.appendTask(this.mgrCheckDevicesMetaTask.create(virtualSystem));
            this.tg.appendTask(this.deleteDsFromDb.create(this.ds));
        } else {
            buildDsUpdateTaskGraph(em);
        }
    }

    private void buildDsUpdateTaskGraph(EntityManager em) throws IOException, Exception {
        log.info("Checking/Updating DS " + this.ds.getName());

        Set<AvailabilityZone> selectedAvailabilityZones = this.ds.getAvailabilityZones();
        Set<HostAggregate> selectedHostAggr = this.ds.getHostAggregates();
        Set<String> selectedHosts = new HashSet<>();
        for (Host host : this.ds.getHosts()) {
            selectedHosts.add(host.getName());
        }

        this.novaApi = this.novaApi == null ? new Openstack4JNova(this.endPoint) : this.novaApi;

        List<? extends org.openstack4j.model.compute.ext.AvailabilityZone> osAvailabilityZones =
                this.novaApi.getAvailabilityZonesDetail(this.ds.getRegion());
        HostAvailabilityZoneMapping hostAvailabilityZoneMap = Openstack4JNova.getMapping(osAvailabilityZones);

        // Openstack hosts are only the hypervisor compute hosts. Ignore hosts from the availability zone api because
        // that would include controller hosts as well.
        Collection<String> osHostSet = this.novaApi.getComputeHosts(this.ds.getRegion());

        if (DeploymentSpecEntityMgr.isDeploymentSpecAllHostInRegion(this.ds)) {
            log.info("Deploying based on region:" + this.ds.getRegion());
            // Deploy to region, one SVA per host in region

            conformToHostsSelection(osHostSet, hostAvailabilityZoneMap, osHostSet);

        } else if (selectedHosts.isEmpty() && !selectedAvailabilityZones.isEmpty()) {
            log.info("Deploying based on availabilityZones");

            conformToAzSelection(em, selectedAvailabilityZones, osAvailabilityZones, osHostSet);

        } else if ((!selectedHosts.isEmpty())) {
            log.info("Deploying based on hosts");
            // Deploy to selected hosts
            conformToHostsSelection(selectedHosts, hostAvailabilityZoneMap, osHostSet);
        } else if (!selectedHostAggr.isEmpty()) {
            log.info("Deploying based on host Aggregate selection");
            // Get selected host set from host aggregates
            Set<String> hostsToDeployTo = getHostsFromHostAggregateSelection(em, this.novaApi, selectedHostAggr);

            // deploy to selected hosts
            conformToHostsSelection(hostsToDeployTo, hostAvailabilityZoneMap, osHostSet);
        }
    }

    private Set<String> getHostsFromHostAggregateSelection(EntityManager em, Openstack4JNova novaApi,
                                                           Set<HostAggregate> dsHostAggr) throws IOException {
        Set<String> hostsToDeployTo = new HashSet<>();
        Iterator<HostAggregate> dsHostAggrIter = dsHostAggr.iterator();
        while (dsHostAggrIter.hasNext()) {
            HostAggregate dsHostAggrInstance = dsHostAggrIter.next();
            org.openstack4j.model.compute.HostAggregate osHostAggr = novaApi.getHostAggregateById(this.ds.getRegion(),
                    dsHostAggrInstance.getOpenstackId());

            if (osHostAggr != null) {
                hostsToDeployTo.addAll(osHostAggr.getHosts());
                // Pigiback to update Host Aggr name in case it changed
                if (!osHostAggr.getName().equals(dsHostAggrInstance.getName())) {
                    dsHostAggrInstance.setName(osHostAggr.getName());
                    OSCEntityManager.update(em, dsHostAggrInstance, this.txBroadcastUtil);
                }
            } else {
                // Host aggr has been deleted, delete the host aggr from ds
                log.info(String.format("Host Aggregate %s(%s) has been deleted from openstack. Deleting from DS.",
                        dsHostAggrInstance.getName(), dsHostAggrInstance.getId()));
                OSCEntityManager.delete(em, dsHostAggrInstance, this.txBroadcastUtil);
                dsHostAggrIter.remove();
                OSCEntityManager.update(em, this.ds, this.txBroadcastUtil);
            }
        }

        return hostsToDeployTo;
    }

    private void conformToHostsSelection(Collection<String> selectedHosts,
                                         HostAvailabilityZoneMapping hostAvailabilityZoneMap, Collection<String> osHostSet) {
        List<String> hostsMissingSvas = new ArrayList<>();

        // TODO: Future. If instance count is decreased, remove least loaded dai's first
        // Assume all hosts need SVA. Add hosts multiple time based on the count
        for (int i = 0; i < this.ds.getInstanceCount(); i++) {
            hostsMissingSvas.addAll(osHostSet);
        }

        Set<DistributedApplianceInstance> existingDais = this.ds.getDistributedApplianceInstances();
        Set<DistributedApplianceInstance> daisToDelete = new HashSet<>();
        for (DistributedApplianceInstance dai : existingDais) {
            String daiHostName = dai.getHostName();
            boolean isDAIHostSelectedInDs = isHostSelected(selectedHosts, daiHostName);
            // if the host missing SVA list contains the dai host, that means the DAI is needed, so conform it and
            // remove it from the list needing SVA's. If its not in the list its not needed so remove it
            boolean doesHostNeedSva = removeHost(hostsMissingSvas, daiHostName);
            if (doesHostNeedSva && isDAIHostSelectedInDs) {
                log.info("Conforming DAI/SVA: " + dai.getName());
                addConformanceCheckTask(dai, osHostSet);
            } else {
                daisToDelete.add(dai);
            }
        }

        for (String host : hostsMissingSvas) {
            if (isHostSelected(selectedHosts, host)) {
                log.info("Creating new SVA for host:" + host);
                try {
                    HostAzInfo hostInfo = hostAvailabilityZoneMap.getHostAvailibilityZoneInfo(host);
                    String hostName = hostInfo.getHostName();
                    String availabilityZone = hostInfo.getAvailabilityZone();
                    addConformanceCheckTask(hostName, availabilityZone);
                } catch (VmidcException vmidcException) {
                    this.tg.addTask(new FailedWithObjectInfoTask(
                            String.format("Create SVA for host '%s' in Region '%s'", host, this.ds.getRegion()),
                            String.format("Host '%s' is not part of any Availability Zone", host),
                            LockObjectReference.getObjectReferences(this.ds)));
                }
            }
        }

        for (DistributedApplianceInstance dai : daisToDelete) {
            // Remove any extra sva/DAI
            log.info("Removing DAI/SVA: %s for host: %s", dai.getName(), dai.getHostName());
            this.tg.addTask(this.deleteSvaServerAndDAIMetaTask.create(this.ds.getRegion(), dai),
                    this.firstCreatePGTask != null ? this.firstCreatePGTask : this.tg.getStartTaskNode().getTask());
        }
    }

    /**
     * Checks if the hostname provided exists in the collection of hosts provided. Openstack returns FQDN of host name
     * for some API calls and just the hostname for other API calls. Direct match of the hostname is not possible.
     * <p>
     * This function matches the hostname even if FQDN's are provided or if FQDN is provided and we need to match it
     * against hostname
     */
    private boolean searchHosts(Collection<String> collectionToSearch, String hostName, boolean remove) {
        Iterator<String> collectionToSearchIterator = collectionToSearch.iterator();
        String normalizedHostName = OpenstackUtil.normalizeHostName(hostName);
        while (collectionToSearchIterator.hasNext()) {
            String listHost = collectionToSearchIterator.next();
            String normalizedListHost = listHost.split("\\.")[0];
            if (listHost.equals(hostName) || normalizedListHost.equals(normalizedHostName)) {
                if (remove) {
                    collectionToSearchIterator.remove();
                }
                return true;
            }
        }
        return false;
    }

    private boolean removeHost(List<String> hostsMissingSvas, String hostName) {
        return searchHosts(hostsMissingSvas, hostName, true);
    }

    private boolean isHostSelected(Collection<String> selectedHosts, String hostName) {
        return searchHosts(selectedHosts, hostName, false);
    }

    private void conformToAzSelection(EntityManager em, Set<AvailabilityZone> selectedAvailabilityZones,
                                      List<? extends org.openstack4j.model.compute.ext.AvailabilityZone> allAvailabilityZones,
                                      Collection<String> osHostSet) throws Exception {
        HostAvailabilityZoneMapping azHostMap = Openstack4JNova.getMapping(allAvailabilityZones);
        Set<String> dsAzDeletedFromOpenstack = new HashSet<>();
        // Assume all AZ's as unconformed initially. Current selected az's are conformed first
        Set<String> unConformedAzs = azHostMap.getAvailabilityZones();

        for (AvailabilityZone az : selectedAvailabilityZones) {
            // Check if the DS availability zone still exists in openstack, if it doesnt, we dont need conformance of
            // DAI's in this zone. Need to remove them, add it to the dsAzDeletedFromOpenstack to remove them later
            boolean isAzStillAvailable = unConformedAzs.remove(az.getZone());
            if (!isAzStillAvailable) {
                dsAzDeletedFromOpenstack.add(az.getZone());
            }

            Set<String> hostsWithinAz = azHostMap.getHosts(az.getZone());
            Set<String> hostsMissingSvas = new HashSet<>();
            if (hostsWithinAz != null) {
                hostsMissingSvas.addAll(hostsWithinAz);
            }

            List<DistributedApplianceInstance> daisInZone = DistributedApplianceInstanceEntityMgr
                    .listByDsIdAndAvailabilityZone(em, this.ds.getId(), az.getZone());

            // For existing DAI in this zone, conform them
            if (daisInZone != null) {
                for (DistributedApplianceInstance dai : daisInZone) {
                    if (!hostsMissingSvas.isEmpty()) {
                        hostsMissingSvas.remove(dai.getOsHostName());
                    }

                    addConformanceCheckTask(dai, osHostSet);
                }
            }

            if (!hostsMissingSvas.isEmpty()) {
                // If any hosts are missing the SVA/DAI, create them
                for (String hostMissingSva : hostsMissingSvas) {
                    log.info("Creating new SVA for host:" + hostMissingSva);

                    addConformanceCheckTask(hostMissingSva, az.getZone());
                }
            }
        }

        // If Any unconformed AZ, these are most likely unselected or were never part of this DS. Add any AZ deleted
        // from openstack to this unconformed list
        unConformedAzs.addAll(dsAzDeletedFromOpenstack);
        for (String unconformedAz : unConformedAzs) {
            List<DistributedApplianceInstance> daisInZone = DistributedApplianceInstanceEntityMgr
                    .listByDsIdAndAvailabilityZone(em, this.ds.getId(), unconformedAz);
            if (daisInZone != null) {
                for (DistributedApplianceInstance dai : daisInZone) {
                    this.tg.addTask(this.deleteSvaServerAndDAIMetaTask.create(this.ds.getRegion(), dai),
                            this.firstCreatePGTask != null ? this.firstCreatePGTask : this.tg.getStartTaskNode().getTask());
                }
            }
        }
    }

    private void addConformanceCheckTask(DistributedApplianceInstance dai, Collection<String> osHostSet) {
        if (this.firstCreatePGTask == null) {
            this.firstCreatePGTask = this.osDAIConformanceCheckMetaTask.create(dai, osHostSet.contains(dai.getOsHostName()));
            this.tg.addTask(this.firstCreatePGTask);
        } else {
            this.tg.addTask(this.osDAIConformanceCheckMetaTask.create(dai, osHostSet.contains(dai.getOsHostName())),
                                                                      this.firstCreatePGTask);
        }
    }

    private void addConformanceCheckTask(String hostMissingSva, String azName) {
        // Make sure the first port-group creating task runs before all the others within the same DS.
        if (this.firstCreatePGTask == null) {
            this.firstCreatePGTask = this.osSvaCreateMetaTask.create(this.ds, hostMissingSva, azName);
            this.tg.addTask(this.firstCreatePGTask);
        } else {
            this.tg.addTask(this.osSvaCreateMetaTask.create(this.ds, hostMissingSva, azName),
                                                            this.firstCreatePGTask);
        }
    }

    @Override
    public String getName() {
        return String.format("Conforming to Deployment Specification '%s'", this.ds.getName());
    }

    @Override
    public TaskGraph getTaskGraph() {
        return this.tg;
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.ds);
    }

}
