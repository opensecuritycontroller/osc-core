package org.osc.core.test.util;

import org.mockito.ArgumentMatcher;
import org.osc.core.broker.job.TaskGraph;

public class TaskGraphMatcher extends ArgumentMatcher<TaskGraph> {
    private TaskGraph expectedTg;

    public TaskGraphMatcher(TaskGraph expectedTg) {
        this.expectedTg = expectedTg;
    }

    @Override
    public boolean matches(Object object) {
        if (object == null || !(object instanceof TaskGraph)) {
            return false;
        }

        TaskGraphHelper.validateTaskGraph(this.expectedTg, (TaskGraph)object);

        return true;
    }
}