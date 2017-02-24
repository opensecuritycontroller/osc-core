/*******************************************************************************
 * Copyright (c) 2017 Intel Corporation
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

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.broker.rest.client.openstack.jcloud.Endpoint;
import org.osc.core.broker.rest.client.openstack.jcloud.JCloudNova;
import org.osc.core.broker.rest.client.openstack.jcloud.JCloudUtil;
import org.osc.core.broker.service.persistence.DistributedApplianceInstanceEntityMgr;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;

class OsSvaDeleteFloatingIpTask extends TransactionalTask {

    private final Logger log = Logger.getLogger(OsSvaDeleteFloatingIpTask.class);

    private DistributedApplianceInstance dai;

    public OsSvaDeleteFloatingIpTask(DistributedApplianceInstance dai) {
        this.dai = dai;
    }

    @Override
    public void executeTransaction(Session session) throws Exception {
        this.dai = DistributedApplianceInstanceEntityMgr.findById(session, this.dai.getId());
        if (this.dai.getIpAddress() == null || this.dai.getFloatingIpId() == null) {
            return;
        }
        DeploymentSpec ds = this.dai.getDeploymentSpec();
        VirtualizationConnector vc = ds.getVirtualSystem().getVirtualizationConnector();

        Endpoint endPoint = new Endpoint(vc, ds.getTenantName());
        JCloudNova nova = new JCloudNova(endPoint);
        try {
            JCloudUtil.deleteFloatingIp(nova, ds.getRegion(), this.dai.getIpAddress(), this.dai.getFloatingIpId());

            this.log.info("Dai: " + this.dai + " Ip Address set to: null");
            this.dai.setIpAddress(null);
            this.dai.setFloatingIpId(null);

            EntityManager.update(session, this.dai);

        } finally {
            nova.close();
        }
    }

    @Override
    public String getName() {
        return String.format("Deleting floating IP assoicated with Distributed Appliance Instance '%s'", this.dai.getName());
    };

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.dai);
    }

}
