package org.osc.core.broker.service.tasks;

import java.util.Set;

import org.osc.core.broker.job.lock.LockObjectReference;

public class InfoTask extends BaseTask {

    private Set<LockObjectReference> references;

    public InfoTask(String name) {
        super(name);
    }

    public InfoTask(String name, Set<LockObjectReference> references) {
        super(name);
        this.references = references;
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return references;
    }

    @Override
    public String toString() {
        return "InfoTask [name=" + name + "]";
    }

}
