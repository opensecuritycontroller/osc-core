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
package org.osc.core.common.job;

/**
 *
 * TaskState represent the execution states that a {@link Task} can be
 * in.
 */
public enum TaskState {
    NOT_RUNNING, // Not scheduled.
    QUEUED, // Scheduled for execution, pending job engine execution thread
    // resource.
    PENDING, // Pending all predecessor to complete.
    RUNNING, // Executing
    COMPLETED; // Execution completed

    public boolean isTerminalState() {
        return equals(COMPLETED);
    }

    public boolean isRunning() {
        return equals(RUNNING);
    }

    public boolean neverScheduled() {
        return equals(NOT_RUNNING);
    }
}
