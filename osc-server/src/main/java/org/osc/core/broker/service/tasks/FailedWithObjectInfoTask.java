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
package org.osc.core.broker.service.tasks;

import java.util.Set;

import org.osc.core.broker.job.lock.LockObjectReference;

public class FailedWithObjectInfoTask extends FailedInfoTask {
    private Set<LockObjectReference> objects;

    public FailedWithObjectInfoTask(String name, String errorStr, Set<LockObjectReference> objects) {
        super(name, errorStr);
        this.objects = objects;
    }

    public FailedWithObjectInfoTask(String name, Exception exception, Set<LockObjectReference> objects) {
        super(name, exception);
        this.objects = objects;
    }

    @Override
    public String toString() {
        return "FailedWithObjectInfoTask [name=" + name + "]";
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return objects;
    }

}
