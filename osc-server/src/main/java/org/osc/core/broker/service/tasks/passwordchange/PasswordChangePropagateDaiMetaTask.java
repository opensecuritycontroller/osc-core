package org.osc.core.broker.service.tasks.passwordchange;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.osc.core.broker.job.Task;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.job.TaskGuard;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.job.lock.LockRequest;
import org.osc.core.broker.job.lock.LockObjectReference.ObjectType;
import org.osc.core.broker.job.lock.LockRequest.LockType;
import org.osc.core.broker.model.entities.appliance.DistributedAppliance;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.rest.server.AgentAuthFilter;
import org.osc.core.broker.service.persistence.DistributedApplianceEntityMgr;
import org.osc.core.broker.service.tasks.TransactionalMetaTask;
import org.osc.core.broker.service.tasks.conformance.LockObjectTask;
import org.osc.core.broker.service.tasks.conformance.UnlockObjectTask;
import org.osc.core.broker.service.tasks.network.UpdateNsxServiceInstanceAttributesTask;
import org.osc.core.util.EncryptionUtil;


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

                    for (DistributedApplianceInstance dai : vs.getDistributedApplianceInstances()) {
                        if (!dai.getPassword().equals(EncryptionUtil.encrypt(AgentAuthFilter.VMIDC_AGENT_PASS))) {
                            propagateTaskGraph.addTask(new PasswordChangePropagateToDaiTask(dai),
                                    TaskGuard.ALL_PREDECESSORS_SUCCEEDED, lockTask);
                        }
                    }
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
