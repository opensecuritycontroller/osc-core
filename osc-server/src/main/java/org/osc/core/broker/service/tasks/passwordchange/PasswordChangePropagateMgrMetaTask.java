package org.osc.core.broker.service.tasks.passwordchange;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.job.TaskGuard;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.job.lock.LockObjectReference.ObjectType;
import org.osc.core.broker.job.lock.LockRequest;
import org.osc.core.broker.job.lock.LockRequest.LockType;
import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.model.plugin.manager.ManagerApiFactory;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.tasks.FailedInfoTask;
import org.osc.core.broker.service.tasks.TransactionalMetaTask;
import org.osc.core.broker.service.tasks.conformance.LockObjectTask;
import org.osc.core.broker.service.tasks.conformance.UnlockObjectTask;
import org.osc.core.broker.service.tasks.conformance.manager.MCConformanceCheckMetaTask;

import com.mcafee.vmidc.server.Server;

public class PasswordChangePropagateMgrMetaTask extends TransactionalMetaTask {

    final static Logger log = Logger.getLogger(PasswordChangePropagateMgrMetaTask.class);
    private TaskGraph tg;

    public PasswordChangePropagateMgrMetaTask() {
        this.name = getName();
    }

    @Override
    public TaskGraph getTaskGraph() {
        return this.tg;
    }

    @Override
    public String getName() {
        return "Updating " + Server.SHORT_PRODUCT_NAME + " password in Manager(s)";
    }

    @Override
    public void executeTransaction(Session session) throws Exception {
        this.tg = new TaskGraph();

        // propagating password change to all MC
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

                log.error("Failed to update password in Security Manager(s) ", e);
                this.tg.addTask(new FailedInfoTask("Syncing password for MC '" + mc.getName() + "'", e));
            }
        }

    }

}
