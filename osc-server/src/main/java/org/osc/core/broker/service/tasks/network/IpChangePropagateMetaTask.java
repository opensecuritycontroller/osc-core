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
package org.osc.core.broker.service.tasks.network;

import javax.persistence.EntityManager;

import org.osc.core.broker.job.Task;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.job.lock.LockObjectReference.ObjectType;
import org.osc.core.broker.job.lock.LockRequest;
import org.osc.core.broker.job.lock.LockRequest.LockType;
import org.osc.core.broker.model.entities.appliance.DistributedAppliance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.tasks.FailedInfoTask;
import org.osc.core.broker.service.tasks.TransactionalMetaTask;
import org.osc.core.broker.service.tasks.conformance.LockObjectTask;
import org.osc.core.broker.service.tasks.conformance.UnlockObjectTask;
import org.osc.core.broker.service.tasks.conformance.manager.MCConformanceCheckMetaTask;
import org.osc.core.broker.service.tasks.conformance.manager.MgrCheckDevicesMetaTask;
import org.slf4j.LoggerFactory;
import org.osc.core.common.job.TaskGuard;
import org.osc.core.server.Server;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;

@Component(service = IpChangePropagateMetaTask.class)
public class IpChangePropagateMetaTask extends TransactionalMetaTask {

    private static final Logger log = LoggerFactory.getLogger(IpChangePropagateMetaTask.class);

    private TaskGraph tg;

    @Reference
    private MCConformanceCheckMetaTask mcConformanceCheckMetaTask;

    @Reference
    private MgrCheckDevicesMetaTask mgrCheckDevicesMetaTask;

    @Reference
    private ApiFactoryService apiFactoryService;

    public IpChangePropagateMetaTask create() {
        IpChangePropagateMetaTask task = new IpChangePropagateMetaTask();
        task.mcConformanceCheckMetaTask = this.mcConformanceCheckMetaTask;
        task.mgrCheckDevicesMetaTask = this.mgrCheckDevicesMetaTask;
        task.name = task.getName();
        task.apiFactoryService = this.apiFactoryService;
        task.dbConnectionManager = this.dbConnectionManager;
        task.txBroadcastUtil = this.txBroadcastUtil;

        return task;
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {

        log.debug("Start executing IP Change Propagate task");

        OSCEntityManager<DistributedAppliance> emgr = new OSCEntityManager<DistributedAppliance>(DistributedAppliance.class,
                em, this.txBroadcastUtil);

        this.tg = new TaskGraph();
        for (DistributedAppliance da : emgr.listAll()) {

            TaskGraph propagateTaskGraph = new TaskGraph();

            LockObjectReference or = new LockObjectReference(da.getId(), da.getName(),
                    ObjectType.DISTRIBUTED_APPLIANCE);
            UnlockObjectTask ult = new UnlockObjectTask(or, LockType.WRITE_LOCK);
            LockRequest lockRequest = new LockRequest(or, ult);
            Task lockTask = new LockObjectTask(lockRequest);
            propagateTaskGraph.addTask(lockTask);

            for (VirtualSystem vs : da.getVirtualSystems()) {
                // Updating Mgr VSS with the updated iSC server IP
                propagateTaskGraph.addTask(this.mgrCheckDevicesMetaTask.create(vs), TaskGuard.ALL_PREDECESSORS_COMPLETED,
                        lockTask);
            }

            propagateTaskGraph.appendTask(ult, TaskGuard.ALL_PREDECESSORS_COMPLETED);
            this.tg.addTaskGraph(propagateTaskGraph);
        }

        OSCEntityManager<ApplianceManagerConnector> emgrMc = new OSCEntityManager<ApplianceManagerConnector>(
                ApplianceManagerConnector.class, em, this.txBroadcastUtil);
        for (ApplianceManagerConnector mc : emgrMc.listAll()) {
            try {
                if (this.apiFactoryService.isPersistedUrlNotifications(mc)) {
                    TaskGraph propagateTaskGraph = new TaskGraph();

                    LockObjectReference or = new LockObjectReference(mc.getId(), mc.getName(),
                            ObjectType.APPLIANCE_MANAGER_CONNECTOR);
                    UnlockObjectTask ult = new UnlockObjectTask(or, LockType.WRITE_LOCK);
                    LockRequest lockRequest = new LockRequest(or, ult);
                    LockObjectTask lockTask = new LockObjectTask(lockRequest);

                    propagateTaskGraph.addTask(lockTask);
                    propagateTaskGraph.addTaskGraph(
                            this.mcConformanceCheckMetaTask.syncPersistedUrlNotification(em, mc), lockTask);
                    propagateTaskGraph.appendTask(ult, TaskGuard.ALL_PREDECESSORS_COMPLETED);
                    this.tg.addTaskGraph(propagateTaskGraph);
                }

            } catch (Exception e) {

                log.error("Failed to update IP in Security Manager(s) ", e);
                this.tg.addTask(
                        new FailedInfoTask("Syncing Notification IP Address for Manager '" + mc.getName() + "'", e));
            }
        }

    }

    @Override
    public String getName() {
        return "Updating " + Server.SHORT_PRODUCT_NAME
                + " IP for Security Manager(s)";
    }

    @Override
    public TaskGraph getTaskGraph() {
        return this.tg;
    }

}
