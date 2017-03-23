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
import org.jclouds.openstack.nova.v2_0.domain.Server;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.broker.rest.client.openstack.jcloud.Endpoint;
import org.osc.core.broker.rest.client.openstack.jcloud.JCloudNova;
import org.osc.core.broker.service.persistence.DistributedApplianceInstanceEntityMgr;
import org.osc.core.broker.service.tasks.TransactionalTask;

class OsSvaStateCheckTask extends TransactionalTask {

    private final Logger log = Logger.getLogger(OsSvaStateCheckTask.class);

    private DistributedApplianceInstance dai;

    public OsSvaStateCheckTask(DistributedApplianceInstance dai) {
        this.dai = dai;
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {
        this.dai = DistributedApplianceInstanceEntityMgr.findById(em, this.dai.getId());
        DeploymentSpec ds = this.dai.getDeploymentSpec();

        Endpoint endPoint = new Endpoint(ds);
        JCloudNova nova = new JCloudNova(endPoint);
        try {
            Server serverDAI = nova.getServer(ds.getRegion(), this.dai.getOsServerId());
            // Check is SVA is Shut off
            if (serverDAI.getStatus().equals(Server.Status.SHUTOFF)) {
                this.log.info("SVA found in SHUTOFF state we will try to start it ...");
                nova.startServer(ds.getRegion(), this.dai.getOsServerId());
            }

        } finally {
            nova.close();
        }
    }

    @Override
    public String getName() {
        return String.format("Checking State for Distributed Appliance Instance '%s'", this.dai.getName());
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.dai);
    }
}
