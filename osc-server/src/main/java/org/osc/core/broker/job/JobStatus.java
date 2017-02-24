package org.osc.core.broker.job;

import org.osc.sdk.manager.element.JobStatusElement;

/**
 *         The JobStatus represent how well a {@link Job} is executing.
 */
public enum JobStatus implements JobStatusElement{
    FAILED, PASSED, ABORTED;

    public boolean isSuccessful() {
        return this.equals(PASSED);
    }

}
