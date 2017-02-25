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
package org.osc.core.broker.service.tasks.passwordchange;

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
import org.osc.core.broker.service.persistence.DistributedApplianceEntityMgr;
import org.osc.core.broker.service.tasks.TransactionalMetaTask;
import org.osc.core.broker.service.tasks.conformance.LockObjectTask;
import org.osc.core.broker.service.tasks.conformance.UnlockObjectTask;
import org.osc.core.broker.service.tasks.network.UpdateNsxServiceInstanceAttributesTask;


public class PasswordChangePropagateDaiMetaTask extends TransactionalMetaTask {

    private static final Logger log = Logger.getLogger(PasswordChangePropagateDaiMetaTask.class);

    private TaskGraph tg;

    public PasswordChangePropagateDaiMetaTask() {
        this.name = getName();
    }

    @Override
    public void executeTransaction(Session session) throws Exception {

        log.debug("Start executing Password Change Propagate DAI task");

        this.tg = new TaskGraph();
        for (DistributedAppliance da : DistributedApplianceEntityMgr.listAllActive(session)) {

            TaskGraph propagateTaskGraph = new TaskGraph();

            LockObjectReference or = new LockObjectReference(da.getId(), da.getName(), ObjectType.DISTRIBUTED_APPLIANCE);
            UnlockObjectTask ult = new UnlockObjectTask(or, LockType.READ_LOCK);
            LockRequest lockRequest = new LockRequest(or, ult);
            Task lockTask = new LockObjectTask(lockRequest);
            propagateTaskGraph.addTask(lockTask);

            for (VirtualSystem vs : da.getVirtualSystems()) {
                if (!vs.getMarkedForDeletion()) {
                    // Updating NSX service attribute 'vmidcPassword' so newly
                    // deployed SVAs has the correct agent password.
                    propagateTaskGraph.addTask(new UpdateNsxServiceAttributesTask(vs),
                            TaskGuard.ALL_PREDECESSORS_SUCCEEDED, lockTask);
                    // Updating NSX service instance attribute "vmidcPassword'
                    propagateTaskGraph.addTask(new UpdateNsxServiceInstanceAttributesTask(vs),
                            TaskGuard.ALL_PREDECESSORS_SUCCEEDED, lockTask);
                }
            }
            propagateTaskGraph.appendTask(ult, TaskGuard.ALL_PREDECESSORS_COMPLETED);
            this.tg.addTaskGraph(propagateTaskGraph);
        }

    }

    @Override
    public String getName() {
        return "Propagating password change to all DAIs and NSX Manager(s)";
    }

    @Override
    public TaskGraph getTaskGraph() {
        return this.tg;
    }

}
