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
package org.osc.core.broker.service.tasks.passwordchange;

import javax.persistence.EntityManager;

import org.apache.log4j.Logger;
import org.osc.core.broker.job.Task;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.job.TaskGuard;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.job.lock.LockRequest;
import org.osc.core.broker.job.lock.LockRequest.LockType;
import org.osc.core.broker.model.entities.appliance.DistributedAppliance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.service.dto.job.ObjectType;
import org.osc.core.broker.service.persistence.DistributedApplianceEntityMgr;
import org.osc.core.broker.service.tasks.TransactionalMetaTask;
import org.osc.core.broker.service.tasks.conformance.LockObjectTask;
import org.osc.core.broker.service.tasks.conformance.UnlockObjectTask;
import org.osc.core.broker.service.tasks.network.UpdateNsxServiceManagerTask;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.mcafee.vmidc.server.Server;

@Component(service = PasswordChangePropagateNsxMetaTask.class)
public class PasswordChangePropagateNsxMetaTask extends TransactionalMetaTask {

    private static final Logger log = Logger.getLogger(PasswordChangePropagateNsxMetaTask.class);

    private TaskGraph tg;

    @Reference
    private UpdateNsxServiceManagerTask updateNsxServiceManagerTask;

    public PasswordChangePropagateNsxMetaTask() {
        this.name = getName();
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {

        log.debug("Start executing Password Change Propagate NSX task");

        this.tg = new TaskGraph();
        for (DistributedAppliance da : DistributedApplianceEntityMgr.listAllActive(em)) {

            TaskGraph propagateTaskGraph = new TaskGraph();

            LockObjectReference or = new LockObjectReference(da.getId(), da.getName(),
                    ObjectType.DISTRIBUTED_APPLIANCE);
            UnlockObjectTask ult = new UnlockObjectTask(or, LockType.READ_LOCK);
            LockRequest lockRequest = new LockRequest(or, ult);
            Task lockTask = new LockObjectTask(lockRequest);
            propagateTaskGraph.addTask(lockTask);

            for (VirtualSystem vs : da.getVirtualSystems()) {
                if (!vs.getMarkedForDeletion()) {
                    propagateTaskGraph.addTask(this.updateNsxServiceManagerTask.create(vs),
                            TaskGuard.ALL_PREDECESSORS_SUCCEEDED, lockTask);
                }
            }

            propagateTaskGraph.appendTask(ult, TaskGuard.ALL_PREDECESSORS_COMPLETED);
            this.tg.addTaskGraph(propagateTaskGraph);
        }

    }

    @Override
    public String getName() {
        return "Updating NSX Manager(s) " + Server.SHORT_PRODUCT_NAME + " password";
    }

    @Override
    public TaskGraph getTaskGraph() {
        return this.tg;
    }

}
