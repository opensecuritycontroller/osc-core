package org.osc.core.broker.job;

import java.util.Set;

import org.osc.core.broker.job.lock.LockObjectReference;

/**
 *         Task interface represents any task which could wired up as part of a {@link TaskGraph} and can be executed by
 *         {@link JobEngine}.
 */
public interface Task {
    String getName();

    void execute() throws Exception;

    Set<LockObjectReference> getObjects();
}
