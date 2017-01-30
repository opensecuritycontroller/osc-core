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
import org.osc.core.broker.service.tasks.network.UpdateNsxServiceManagerTask;

import com.mcafee.vmidc.server.Server;

public class PasswordChangePropagateNsxMetaTask extends TransactionalMetaTask {

    private static final Logger log = Logger.getLogger(PasswordChangePropagateNsxMetaTask.class);

    private TaskGraph tg;

    public PasswordChangePropagateNsxMetaTask() {
        this.name = getName();
    }

    @Override
    public void executeTransaction(Session session) throws Exception {

        log.debug("Start executing Password Change Propagate NSX task");

        this.tg = new TaskGraph();
        for (DistributedAppliance da : DistributedApplianceEntityMgr.listAllActive(session)) {

            TaskGraph propagateTaskGraph = new TaskGraph();

            LockObjectReference or = new LockObjectReference(da.getId(), da.getName(),
                    ObjectType.DISTRIBUTED_APPLIANCE);
            UnlockObjectTask ult = new UnlockObjectTask(or, LockType.READ_LOCK);
            LockRequest lockRequest = new LockRequest(or, ult);
            Task lockTask = new LockObjectTask(lockRequest);
            propagateTaskGraph.addTask(lockTask);

            for (VirtualSystem vs : da.getVirtualSystems()) {
                if (!vs.getMarkedForDeletion()) {
                    propagateTaskGraph.addTask(new UpdateNsxServiceManagerTask(vs),
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
