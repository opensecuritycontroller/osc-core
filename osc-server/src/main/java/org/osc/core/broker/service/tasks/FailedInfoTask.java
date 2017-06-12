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


public class FailedInfoTask extends BaseTask {
    protected String errorStr;
    private Exception exception;

    public FailedInfoTask(String name, String errorStr) {
        super(name);
        this.errorStr = errorStr;
    }

    public FailedInfoTask(String name, Exception exception) {
        super(name);
        this.exception = exception;
    }

    @Override
    public void execute() throws Exception {
        if (exception != null) {
            throw exception;
        } else {
            throw new Exception(errorStr);
        }
    }

    @Override
    public String toString() {
        return "FailedInfoTask [name=" + name + "]";
    }

}
