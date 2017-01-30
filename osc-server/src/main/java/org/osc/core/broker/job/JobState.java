package org.osc.core.broker.job;

import org.osc.sdk.manager.element.JobStateElement;

/**

 *         JobState represent the execution states that a {@link Job} can be in.
 */
public enum JobState implements JobStateElement {
    NOT_RUNNING, QUEUED, RUNNING, COMPLETED;

    public boolean isTerminalState() {
        return this.equals(COMPLETED);
    }

    public boolean isRunning() {
        return this.equals(RUNNING);
    }

}
