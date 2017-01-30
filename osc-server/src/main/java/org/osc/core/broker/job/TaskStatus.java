package org.osc.core.broker.job;

import org.osc.sdk.manager.element.TaskStatusElement;

/**

 *         The TaskStatus represent how well a {@link Task} is executing.
 */
public enum TaskStatus implements TaskStatusElement {
    FAILED, // Execution logic encountered an error.
    SKIPPED, // Execution skipped due to dependency constraints (for example,
             // not all dependent task were successful).
    PASSED, // Execution logic completed successfully.
    ABORTED;

    public boolean isSuccessful() {
        return this.equals(PASSED);
    }
}
