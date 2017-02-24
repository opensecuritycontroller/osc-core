/*******************************************************************************
 * Copyright (c) 2017 Intel Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
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
