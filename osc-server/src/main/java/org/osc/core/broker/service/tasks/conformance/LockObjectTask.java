package org.osc.core.broker.service.tasks.conformance;

import org.osc.core.broker.job.lock.LockManager;
import org.osc.core.broker.job.lock.LockRequest;
import org.osc.core.broker.service.LockUtil;
import org.osc.core.broker.service.tasks.BaseTask;

public class LockObjectTask extends BaseTask {

    private LockRequest lockRequest;

    public LockObjectTask(LockRequest lockRequest) {
        super(getName(lockRequest));
        this.lockRequest = lockRequest;
    }

    private static String getName(LockRequest lockRequest) {
        return "Place " + lockRequest.getLockType() + " on Object '" + lockRequest.getObjectRef().getName() + "' ("
                + lockRequest.getObjectRef().getType() + ")";
    }

    @Override
    public void execute() throws Exception {
        LockManager.getLockManager().acquireLock(lockRequest, LockUtil.DEFAULT_MAX_LOCK_TIMEOUT);
    }

    @Override
    public String toString() {
        return "LockObjectTask [" + lockRequest + "]";
    }

}
