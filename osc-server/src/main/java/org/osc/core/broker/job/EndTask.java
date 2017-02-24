package org.osc.core.broker.job;

import org.osc.core.broker.service.tasks.BaseTask;

/**
 *         The EndTask is a special kind of {@link Task} which automatically
 *         gets added to every {@link TaskGraph} and is always the last task to
 *         be executed.
 */
public class EndTask extends BaseTask {

    public EndTask() {
        super("EndTask");
    }

    @Override
    public void execute() {
    }

    @Override
    public String toString() {
        return "EndTask";
    }

}
