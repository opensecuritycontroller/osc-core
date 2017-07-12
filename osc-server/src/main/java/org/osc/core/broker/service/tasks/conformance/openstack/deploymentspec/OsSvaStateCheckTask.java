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

import org.apache.log4j.Logger;
import org.openstack4j.model.compute.Server;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.broker.rest.client.openstack.openstack4j.Endpoint;
import org.osc.core.broker.rest.client.openstack.openstack4j.Openstack4JNova;
import org.osc.core.broker.service.persistence.DistributedApplianceInstanceEntityMgr;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osgi.service.component.annotations.Component;

import javax.persistence.EntityManager;
import java.util.Set;

@Component(service = OsSvaStateCheckTask.class)
public class OsSvaStateCheckTask extends TransactionalTask {

    private final Logger log = Logger.getLogger(OsSvaStateCheckTask.class);

    private DistributedApplianceInstance dai;

    public OsSvaStateCheckTask create(DistributedApplianceInstance dai) {
        OsSvaStateCheckTask task = new OsSvaStateCheckTask();
        task.dai = dai;
        task.dbConnectionManager = this.dbConnectionManager;
        task.txBroadcastUtil = this.txBroadcastUtil;

        return task;
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {
        this.dai = DistributedApplianceInstanceEntityMgr.findById(em, this.dai.getId());
        DeploymentSpec ds = this.dai.getDeploymentSpec();

        Endpoint endPoint = new Endpoint(ds);
        try (Openstack4JNova nova = new Openstack4JNova(endPoint)) {
            Server serverDAI = nova.getServer(ds.getRegion(), this.dai.getOsServerId());
            // Check is SVA is Shut off
            if (serverDAI.getStatus().equals(Server.Status.SHUTOFF)) {
                boolean isStarted = nova.startServer(ds.getRegion(), this.dai.getOsServerId());
                this.log.info("SVA found in SHUTOFF state we will try to start it ... Is SVA started successfully: " + isStarted);
            }
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
