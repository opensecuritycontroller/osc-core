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

import org.apache.log4j.Logger;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.broker.rest.client.openstack.openstack4j.Endpoint;
import org.osc.core.broker.rest.client.openstack.openstack4j.Openstack4JNova;
import org.osc.core.broker.service.persistence.DistributedApplianceInstanceEntityMgr;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osgi.service.component.annotations.Component;

@Component(service=OsSvaDeleteFloatingIpTask.class)
public class OsSvaDeleteFloatingIpTask extends TransactionalTask {

    private final Logger log = Logger.getLogger(OsSvaDeleteFloatingIpTask.class);

    private DistributedApplianceInstance dai;

    public OsSvaDeleteFloatingIpTask create(DistributedApplianceInstance dai) {
        OsSvaDeleteFloatingIpTask task = new OsSvaDeleteFloatingIpTask();
        task.dai = dai;
        task.dbConnectionManager = this.dbConnectionManager;
        task.txBroadcastUtil = this.txBroadcastUtil;

        return task;
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {
        this.dai = DistributedApplianceInstanceEntityMgr.findById(em, this.dai.getId());
        if (this.dai.getIpAddress() == null || this.dai.getFloatingIpId() == null) {
            return;
        }
        DeploymentSpec ds = this.dai.getDeploymentSpec();
        VirtualizationConnector vc = ds.getVirtualSystem().getVirtualizationConnector();

        Endpoint endPoint = new Endpoint(vc, ds.getTenantName());
        Openstack4JNova nova = new Openstack4JNova(endPoint);
        nova.deleteFloatingIp(ds.getRegion(), this.dai.getIpAddress(), this.dai.getOsServerId());

        this.log.info("Dai: " + this.dai + " Ip Address set to: null");
        this.dai.setIpAddress(null);
        this.dai.setFloatingIpId(null);

        OSCEntityManager.update(em, this.dai, this.txBroadcastUtil);

    }

    @Override
    public String getName() {
        return String.format("Deleting floating IP assoicated with Distributed Appliance Instance '%s'", this.dai.getName());
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.dai);
    }

}
