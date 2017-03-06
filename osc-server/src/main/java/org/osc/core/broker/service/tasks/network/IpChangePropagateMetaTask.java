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

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.osc.core.broker.job.Task;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.job.TaskGuard;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.job.lock.LockObjectReference.ObjectType;
import org.osc.core.broker.job.lock.LockRequest;
import org.osc.core.broker.job.lock.LockRequest.LockType;
import org.osc.core.broker.model.entities.appliance.DistributedAppliance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.appliance.VirtualizationType;
import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.model.plugin.manager.ManagerApiFactory;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.tasks.FailedInfoTask;
import org.osc.core.broker.service.tasks.TransactionalMetaTask;
import org.osc.core.broker.service.tasks.conformance.LockObjectTask;
import org.osc.core.broker.service.tasks.conformance.UnlockObjectTask;
import org.osc.core.broker.service.tasks.conformance.manager.MCConformanceCheckMetaTask;
import org.osc.core.broker.service.tasks.conformance.manager.MgrCheckDevicesMetaTask;
import org.osc.core.broker.service.tasks.passwordchange.UpdateNsxServiceAttributesTask;

import com.mcafee.vmidc.server.Server;

public class IpChangePropagateMetaTask extends TransactionalMetaTask {

    private static final Logger log = Logger.getLogger(IpChangePropagateMetaTask.class);

    private TaskGraph tg;

    public IpChangePropagateMetaTask() {
        this.name = getName();
    }

    @Override
    public void executeTransaction(Session session) throws Exception {

        log.debug("Start executing IP Change Propagate task");

        EntityManager<DistributedAppliance> emgr = new EntityManager<DistributedAppliance>(DistributedAppliance.class,
                session);

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

                if (vs.getVirtualizationConnector().getVirtualizationType() == VirtualizationType.VMWARE) {
                    // Updating Service Manager callback URL
                    propagateTaskGraph.addTask(new UpdateNsxServiceManagerTask(vs),
                            TaskGuard.ALL_PREDECESSORS_SUCCEEDED, lockTask);
                    // Updating Service Attribute which include vmiDC server IP
                    propagateTaskGraph.addTask(new UpdateNsxServiceAttributesTask(vs),
                            TaskGuard.ALL_PREDECESSORS_SUCCEEDED, lockTask);
                    // Updating Service Deployment Spec OVF Image URL
                    propagateTaskGraph.addTask(new UpdateNsxDeploymentSpecTask(vs),
                            TaskGuard.ALL_PREDECESSORS_SUCCEEDED, lockTask);
                    // Updating Service Instance Attributes which include vmiDC server IP
                    propagateTaskGraph.addTask(new UpdateNsxServiceInstanceAttributesTask(vs),
                            TaskGuard.ALL_PREDECESSORS_SUCCEEDED, lockTask);
                }

                // Updating Mgr VSS with the updated iSC server IP
                propagateTaskGraph.addTask(new MgrCheckDevicesMetaTask(vs), TaskGuard.ALL_PREDECESSORS_COMPLETED,
                        lockTask);
            }

            propagateTaskGraph.appendTask(ult, TaskGuard.ALL_PREDECESSORS_COMPLETED);
            this.tg.addTaskGraph(propagateTaskGraph);
        }

        EntityManager<ApplianceManagerConnector> emgrMc = new EntityManager<ApplianceManagerConnector>(
                ApplianceManagerConnector.class, session);
        for (ApplianceManagerConnector mc : emgrMc.listAll()) {
            try {
                if (ManagerApiFactory.isPersistedUrlNotifications(mc)) {
                    TaskGraph propagateTaskGraph = new TaskGraph();

                    LockObjectReference or = new LockObjectReference(mc.getId(), mc.getName(),
                            ObjectType.APPLIANCE_MANAGER_CONNECTOR);
                    UnlockObjectTask ult = new UnlockObjectTask(or, LockType.WRITE_LOCK);
                    LockRequest lockRequest = new LockRequest(or, ult);
                    LockObjectTask lockTask = new LockObjectTask(lockRequest);

                    propagateTaskGraph.addTask(lockTask);
                    propagateTaskGraph.addTaskGraph(
                            MCConformanceCheckMetaTask.syncPersistedUrlNotification(session, mc), lockTask);
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
                + " IP for Appliance Instance(s), Security Manager(s) and NSX Manager(s)";
    }

    @Override
    public TaskGraph getTaskGraph() {
        return this.tg;
    }

}
