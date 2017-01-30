package org.osc.core.broker.service.tasks;

import java.util.Set;

import org.osc.core.broker.job.Task;
import org.osc.core.broker.job.lock.LockObjectReference;

public class BaseTask implements Task {
    protected String name;

    public BaseTask(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void execute() throws Exception {
    }

    @Override
    public String toString() {
        return "BaseTask [name=" + name + "]";
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return null;
    }

}
