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

import java.util.Set;

import javax.persistence.EntityManager;

import org.apache.commons.lang.StringUtils;
import org.openstack4j.model.network.NetFloatingIP;
import org.openstack4j.model.network.Network;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.broker.rest.client.openstack.openstack4j.Endpoint;
import org.osc.core.broker.rest.client.openstack.openstack4j.Openstack4JNeutron;
import org.osc.core.broker.service.persistence.DistributedApplianceInstanceEntityMgr;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.tasks.InfoTask;
import org.osc.core.broker.service.tasks.TransactionalMetaTask;
import org.osc.core.broker.util.log.LogProvider;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;

@Component(service = OsSvaCheckFloatingIpTask.class)
public class OsSvaCheckFloatingIpTask extends TransactionalMetaTask {

    private final Logger log = LogProvider.getLogger(OsSvaCheckFloatingIpTask.class);

    private TaskGraph tg;
    private DistributedApplianceInstance dai;

    public OsSvaCheckFloatingIpTask create(DistributedApplianceInstance dai) {
        OsSvaCheckFloatingIpTask task = new OsSvaCheckFloatingIpTask();
        task.dai = dai;
        task.dbConnectionManager = this.dbConnectionManager;
        task.txBroadcastUtil = this.txBroadcastUtil;

        return task;
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {
        this.tg = new TaskGraph();
        this.dai = DistributedApplianceInstanceEntityMgr.findById(em, this.dai.getId());
        DeploymentSpec ds = this.dai.getDeploymentSpec();

        Endpoint endPoint = new Endpoint(ds);
        String infoTaskName = String.format("Adding floating IP to SVA for Distributed Appliance Instance '%s'",
                this.dai.getName());

        try (Openstack4JNeutron neutron = new Openstack4JNeutron(endPoint)) {
            // Check if floating ip is assigned to SVA
            NetFloatingIP floatingIp = neutron.getFloatingIp(ds.getRegion(), this.dai.getFloatingIpId());
            if (floatingIp != null) {
                // Floating ip is assigned to instance
                if (StringUtils.isNotBlank(floatingIp.getFixedIpAddress())) {
                    //Floating ip instance is different than dai management port
                    if (!floatingIp.getPortId().equals(this.dai.getMgmtOsPortId())) {
                        this.log.info("Original Floating ip: " + floatingIp.getFloatingIpAddress() + " has been reassigned to another server for" + " DAI: " + this.dai.getName());
                        throw new IllegalStateException("No Floating Ip assigned to instance. Please assign the original Floating ip: " + floatingIp.getFloatingIpAddress() + " to this instance to fix the issue.");
                    }
                } else {
                    // Floating Ip was associated with this DAI before, assign the ip back to it if its free
                    neutron.associateMgmtPortWithFloatingIp(ds.getRegion(), floatingIp.getId(), this.dai.getMgmtOsPortId());
                }
                this.tg.addTask(new InfoTask(infoTaskName, LockObjectReference.getObjectReferences(this.dai)));
            } else {
                Network floatingPoolNetwork = neutron.getNetworkByName(ds.getRegion(), ds.getFloatingIpPoolName());
                // Floating ip is invalid or has never been assigned to this sva for some reason, try adding it now.
                NetFloatingIP allocatedFloatingIp = neutron.createFloatingIp(ds.getRegion(),
                        floatingPoolNetwork.getId(), this.dai.getExternalId(), this.dai.getMgmtOsPortId());
                this.dai.setIpAddress(allocatedFloatingIp.getFloatingIpAddress());
                this.dai.setFloatingIpId(allocatedFloatingIp.getId());
                this.log.info("Dai: " + this.dai + " Ip Address set to: " + allocatedFloatingIp);
                this.tg.addTask(new InfoTask(infoTaskName, LockObjectReference.getObjectReferences(this.dai)));

                OSCEntityManager.update(em, this.dai, this.txBroadcastUtil);
            }
        }

    }

    @Override
    public String getName() {
        return String.format("Checking SVA floating IP for Distributed Appliance Instance '%s'", this.dai.getName());
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.dai);
    }

    @Override
    public TaskGraph getTaskGraph() {
        return this.tg;
    }

}
