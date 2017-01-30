package org.osc.core.broker.job;

import org.osc.core.broker.service.tasks.BaseTask;

/**
 *         The StartTask is a special kind of {@link Task} which automatically
 *         gets added to every TaskGraph and is always the first task to be
 *         executed.
 */
public class StartTask extends BaseTask {

    public StartTask() {
        super("StartTask");
    }

    @Override
    public void execute() {
    }

    @Override
    public String toString() {
        return "StartTask";
    }

}
