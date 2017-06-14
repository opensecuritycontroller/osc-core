/*******************************************************************************
 * Copyright (c) Intel Corporation
 * Copyright (c) 2017
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
package org.osc.core.broker.service.tasks;

import java.util.Set;

import org.osc.core.broker.job.Task;
import org.osc.core.broker.job.lock.LockObjectReference;

public class BaseTask implements Task {
    protected String name;

    public BaseTask(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void execute() throws Exception {
    }

    @Override
    public String toString() {
        return "BaseTask [name=" + name + "]";
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return null;
    }

}
