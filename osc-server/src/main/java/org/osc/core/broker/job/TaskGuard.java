package org.osc.core.broker.job;

/**

 *         TaskGuard defines a {@link Task} conditional execution based on the
 *         task's predecessors.
 */
public enum TaskGuard {
    ALL_ANCESTORS_SUCCEEDED, // All tasks(not just its predecessors) before this task must be successfully completed
    // ANY_PREDECESSORS_SUCCEEDED, // Allow task to start execution when any of
    // its predecessors succeeded.
    ALL_PREDECESSORS_COMPLETED, // All predecessor tasks must be in a completed
                                // state.
    ALL_PREDECESSORS_SUCCEEDED; // All predecessor tasks must be completed
                                // successfully.
}
