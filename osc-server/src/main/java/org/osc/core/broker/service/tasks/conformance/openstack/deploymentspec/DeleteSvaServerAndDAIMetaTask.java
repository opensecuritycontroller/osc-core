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
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.DistributedApplianceInstanceEntityMgr;
import org.osc.core.broker.service.tasks.TransactionalMetaTask;
import org.osc.core.broker.service.tasks.conformance.deleteda.DeleteDAIFromDbTask;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = DeleteSvaServerAndDAIMetaTask.class)
public class DeleteSvaServerAndDAIMetaTask extends TransactionalMetaTask {

    @Reference
    private DeleteDAIFromDbTask deleteDAIFromDbTask;

    @Reference
    private DeleteInspectionPortTask deleteInspectionPort;

    @Reference
    private DeleteSvaServerTask deleteSvaServer;

    @Reference
    private OsSvaDeleteFloatingIpTask osSvadeleteFloatingIp;

    @Reference
    private ApiFactoryService apiFactoryService;

    private DistributedApplianceInstance dai;
    private String region;
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
    public DeleteSvaServerAndDAIMetaTask create(String region, DistributedApplianceInstance dai) {
        DeleteSvaServerAndDAIMetaTask task = new DeleteSvaServerAndDAIMetaTask();
        task.region = region;
        task.dai = dai;

        task.deleteDAIFromDbTask = this.deleteDAIFromDbTask;
        task.deleteInspectionPort = this.deleteInspectionPort;
        task.deleteSvaServer = this.deleteSvaServer;
        task.osSvadeleteFloatingIp = this.osSvadeleteFloatingIp;
        task.apiFactoryService = this.apiFactoryService;

        task.dbConnectionManager = this.dbConnectionManager;
        task.txBroadcastUtil = this.txBroadcastUtil;

        return task;
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {
        this.tg = new TaskGraph();
        this.dai = DistributedApplianceInstanceEntityMgr.findById(em, this.dai.getId());

        if (this.dai.getProtectedPorts() != null && !this.dai.getProtectedPorts().isEmpty()) {
            throw new VmidcBrokerValidationException("Server is being actively used to protect other servers");
        }

        this.tg.appendTask(this.deleteInspectionPort.create(this.region, this.dai));

        this.tg.addTask(this.deleteSvaServer.create(this.region, this.dai));
        if (this.dai.getFloatingIpId() != null) {
            this.tg.appendTask(this.osSvadeleteFloatingIp.create(this.dai));
        }

        this.tg.appendTask(this.deleteDAIFromDbTask.create(this.dai));
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
