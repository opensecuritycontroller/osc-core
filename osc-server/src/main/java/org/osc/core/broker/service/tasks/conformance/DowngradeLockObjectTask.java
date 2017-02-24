package org.osc.core.broker.service.tasks.conformance;

import org.hibernate.Session;
import org.osc.core.broker.job.lock.LockManager;
import org.osc.core.broker.job.lock.LockRequest;
import org.osc.core.broker.service.tasks.TransactionalTask;

public class DowngradeLockObjectTask extends TransactionalTask {

    private LockRequest lockRequest;

    public DowngradeLockObjectTask(LockRequest lockRequest) {
        this.lockRequest = lockRequest;
    }

    @Override
    public String getName() {
        return "Downgrade To '" + this.lockRequest.getLockType() + "' for Lock Object '"
                + this.lockRequest.getObjectRef().getName() + "' (" + this.lockRequest.getObjectRef().getType() + ")";
    }

    @Override
    public void executeTransaction(Session session) throws Exception {
        LockManager.getLockManager().downgradeLock(this.lockRequest);
    }

}
