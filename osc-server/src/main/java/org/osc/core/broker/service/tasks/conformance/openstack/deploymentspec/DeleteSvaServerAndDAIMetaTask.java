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
import java.util.concurrent.atomic.AtomicBoolean;

import javax.persistence.EntityManager;

import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.plugin.sdncontroller.SdnControllerApiFactory;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.DistributedApplianceInstanceEntityMgr;
import org.osc.core.broker.service.tasks.IgnoreCompare;
import org.osc.core.broker.service.tasks.TransactionalMetaTask;
import org.osc.core.broker.service.tasks.conformance.deleteda.DeleteDAIFromDbTask;
import org.osgi.service.component.ComponentServiceObjects;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

@Component(service = DeleteSvaServerAndDAIMetaTask.class)
public class DeleteSvaServerAndDAIMetaTask extends TransactionalMetaTask {

    // optional+dynamic to break circular DS dependency
    // TODO: remove circularity and use mandatory references
    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile ComponentServiceObjects<DeleteDAIFromDbTask> deleteDAIFromDbTaskCSO;

    private DeleteDAIFromDbTask deleteDAIFromDbTask;

    @Reference
    private DeleteInspectionPortTask deleteInspectionPort;
    @Reference
    private DeleteSvaServerTask deleteSvaServer;
    @Reference
    private OsSvaDeleteFloatingIpTask osSvadeleteFloatingIp;

    private DistributedApplianceInstance dai;
    private String region;
    private TaskGraph tg;
    @IgnoreCompare
    private DeleteSvaServerAndDAIMetaTask factory;
    @IgnoreCompare
    private AtomicBoolean initDone = new AtomicBoolean();

    private void delayedInit() {
        if (this.initDone.compareAndSet(false, true)) {
            this.deleteDAIFromDbTask = this.factory.deleteDAIFromDbTaskCSO.getService();
        }
    }

    @Deactivate
    private void deactivate() {
        if (this.initDone.get()) {
            this.factory.deleteDAIFromDbTaskCSO.ungetService(this.deleteDAIFromDbTask);
        }
    }

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
        task.factory = this;
        task.region = region;
        task.dai = dai;
        task.deleteInspectionPort = this.deleteInspectionPort;
        task.deleteSvaServer = this.deleteSvaServer;
        task.osSvadeleteFloatingIp = this.osSvadeleteFloatingIp;
        task.dbConnectionManager = this.dbConnectionManager;
        task.txBroadcastUtil = this.txBroadcastUtil;

        return task;
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {
        delayedInit();
        this.tg = new TaskGraph();
        this.dai = DistributedApplianceInstanceEntityMgr.findById(em, this.dai.getId());

        if (this.dai.getProtectedPorts() != null && !this.dai.getProtectedPorts().isEmpty()) {
            throw new VmidcBrokerValidationException("Server is being actively used to protect other servers");
        }
        if (SdnControllerApiFactory.supportsPortGroup(this.dai.getVirtualSystem())){
            this.tg.appendTask(this.deleteInspectionPort.create(this.region, this.dai));
        }
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
