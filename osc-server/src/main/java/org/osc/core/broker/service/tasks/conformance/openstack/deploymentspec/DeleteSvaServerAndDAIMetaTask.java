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

import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.plugin.sdncontroller.SdnControllerApiFactory;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.DistributedApplianceInstanceEntityMgr;
import org.osc.core.broker.service.tasks.TransactionalMetaTask;
import org.osc.core.broker.service.tasks.conformance.deleteda.DeleteDAIFromDbTask;

class DeleteSvaServerAndDAIMetaTask extends TransactionalMetaTask {

    private DistributedApplianceInstance dai;
    private final String region;
    private TaskGraph tg;

    /**
     * Deletes the SVA associated with the DAI from openstack and deletes the DAI from the DB
     *
     * @param region
     *            the region the sva belongs to
     * @param serverId
     *            the server id
     * @param daiId
     *            the dai id
     * @param osEndPoint
     */
    public DeleteSvaServerAndDAIMetaTask(String region, DistributedApplianceInstance dai) {
        this.region = region;
        this.dai = dai;
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {
        this.tg = new TaskGraph();
        this.dai = DistributedApplianceInstanceEntityMgr.findById(em, this.dai.getId());

        if (this.dai.getProtectedPorts() != null && !this.dai.getProtectedPorts().isEmpty()) {
            throw new VmidcBrokerValidationException("Server is being actively used to protect other servers");
        }
        if (SdnControllerApiFactory.supportsPortGroup(this.dai.getVirtualSystem())){
            this.tg.appendTask(new DeleteInspectionPortTask(this.region, this.dai));
        }
        this.tg.addTask(new DeleteSvaServerTask(this.region, this.dai));
        if (this.dai.getFloatingIpId() != null) {
            this.tg.appendTask(new OsSvaDeleteFloatingIpTask(this.dai));
        }
        this.tg.appendTask(new DeleteDAIFromDbTask(this.dai));
    }

    @Override
    public String getName() {
        return String.format("Deleting Distributed Appliance Instance and Server instance '%s' from region '%s'",
                this.dai.getName(), this.region);
    }

    @Override
    public TaskGraph getTaskGraph() {
        return this.tg;
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.dai);
    }

}
