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
