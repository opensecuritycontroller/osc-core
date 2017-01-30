package org.osc.core.broker.job.lock;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.osc.core.broker.job.Task;
import org.osc.core.broker.service.tasks.conformance.UnlockObjectTask;

public class LockRequest {

    public enum LockType {
        READ_LOCK("Read Lock"), WRITE_LOCK("Write Lock"), UNKNOWN_LOCK("Unknown Lock");

        private String type;

        private LockType(String type) {
            this.type = type;
        }

        @Override
        public String toString() {
            return this.type;
        }
    }

    private LockObjectReference objectRef;
    private UnlockObjectTask unlockTask;

    public LockRequest(LockObjectReference objectRef, UnlockObjectTask unlockTask) {
        this.objectRef = objectRef;
        this.unlockTask = unlockTask;
    }

    public LockRequest(UnlockObjectTask unlockTask) {
        this.unlockTask = unlockTask;
        this.objectRef = unlockTask.getObjectRef();
    }

    public LockObjectReference getObjectRef() {
        return this.objectRef;
    }

    public LockType getLockType() {
        return this.unlockTask.getLockType();
    }

    public void setLockType(LockType lockType) {
        this.unlockTask.setLockType(lockType);
    }

    public Task getUnlockTask() {
        return this.unlockTask;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(this.objectRef).append(this.unlockTask).toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof LockRequest)) {
            return false;
        }

        LockRequest that = (LockRequest) obj;
        return new EqualsBuilder().append(this.objectRef, that.objectRef).append(getUnlockTask(), that.getUnlockTask())
                .isEquals();
    }

    @Override
    public String toString() {
        return "LockRequest [objectRef=" + this.objectRef + ", unlockTask="+ this.unlockTask + "]";
    }

}
