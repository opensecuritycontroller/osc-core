package org.osc.core.broker.service.tasks;

import java.util.Set;

import org.osc.core.broker.job.lock.LockObjectReference;

public class FailedWithObjectInfoTask extends FailedInfoTask {
    private Set<LockObjectReference> objects;

    public FailedWithObjectInfoTask(String name, String errorStr, Set<LockObjectReference> objects) {
        super(name, errorStr);
        this.objects = objects;
    }

    public FailedWithObjectInfoTask(String name, Exception exception, Set<LockObjectReference> objects) {
        super(name, exception);
        this.objects = objects;
    }

    @Override
    public String toString() {
        return "FailedWithObjectInfoTask [name=" + name + "]";
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return objects;
    }

}
