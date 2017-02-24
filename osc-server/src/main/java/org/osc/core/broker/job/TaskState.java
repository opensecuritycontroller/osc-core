package org.osc.core.broker.job;

import org.osc.sdk.manager.element.TaskStateElement;

/**
 *
 * TaskState represent the execution states that a {@link Task} can be
 * in.
 */

public enum TaskState implements TaskStateElement {
    NOT_RUNNING, // Not scheduled.
    QUEUED, // Scheduled for execution, pending job engine execution thread
            // resource.
    PENDING, // Pending all predecessor to complete.
    RUNNING, // Executing
    COMPLETED; // Execution completed

    public boolean isTerminalState() {
        return this.equals(COMPLETED);
    }

    public boolean isRunning() {
        return this.equals(RUNNING);
    }

    public boolean neverScheduled() {
        return this.equals(NOT_RUNNING);
    }

}
