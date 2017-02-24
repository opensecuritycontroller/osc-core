package org.osc.core.broker.service.tasks.conformance;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.osc.core.broker.job.MetaTask;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.job.TaskGuard;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.job.lock.LockObjectReference.ObjectType;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;

public class UnlockObjectMetaTask implements MetaTask {

    private List<UnlockObjectTask> unlockTasks;
    private TaskGraph tg;


    public UnlockObjectMetaTask(List<UnlockObjectTask> unlockTasks) {
        this.unlockTasks = unlockTasks;
    }

    @Override
    public void execute() throws Exception {
        this.tg = new TaskGraph();
        for (UnlockObjectTask unlockTask : this.unlockTasks) {
            this.tg.appendTask(unlockTask, TaskGuard.ALL_PREDECESSORS_COMPLETED);
        }
    }

    @Override
    public TaskGraph getTaskGraph() {
        return this.tg;
    }

    @Override
    public String getName() {
        return "Unlock objects";
    }

    /**
     * Gets the lock object references held by the tasks within this meta task
     */
    public Collection<LockObjectReference> getLockObjectReferences() {
        return Collections2.transform(this.unlockTasks, new Function<UnlockObjectTask, LockObjectReference>() {

            @Override
            public LockObjectReference apply(UnlockObjectTask input) {
                return input.getObjectRef();
            }
        });
    }

    public List<UnlockObjectTask> getUnlockTasks() {
        return this.unlockTasks;
    }

    /**
     * Return the lock task for the specified object and id. Throws an IllegalArgumentException
     * in case the lock is not found within locks specified in the meta task.
     */
    public UnlockObjectTask getUnlockTaskByTypeAndId(ObjectType type, Long id) {
        for (UnlockObjectTask unlockTask : this.unlockTasks) {
            LockObjectReference objectRef = unlockTask.getObjectRef();
            if (objectRef.getType() == type && objectRef.getId().equals(id)) {
                return unlockTask;
            }
        }
        throw new IllegalArgumentException("Specified lock not found in the list of locks");
    }

    public void addUnlockTask(UnlockObjectTask unlockTask) {
        this.unlockTasks.add(unlockTask);
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return null;
    }

    @Override
    public String toString() {
        return "UnlockObjectMetaTask [unlockTasks=" + this.unlockTasks + "]";
    }

}
