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
package org.osc.core.broker.service.dto;

import java.io.Serializable;

public class TaskFailureRecordDto implements Serializable {

    private static final long serialVersionUID = -9045645486832978243L;

    private String taskFailureReason;
    private long taskFailureCount;

    public TaskFailureRecordDto(String taskFailureReason, long taskFailureCount) {
        this.taskFailureReason = taskFailureReason;
        this.taskFailureCount = taskFailureCount;
    }

    public String getTaskFailureReason() {
        return this.taskFailureReason;
    }

    public long getTaskFailureCount() {
        return this.taskFailureCount;
    }

}