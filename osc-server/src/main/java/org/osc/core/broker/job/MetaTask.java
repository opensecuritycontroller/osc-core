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

/**
 * A {@link Task} that constructs other Tasks (in the form of a {@link TaskGraph}) instead of executing business logic.
 * 
 * The <tt>TaskGraph</tt> constructed by this <tt>MetaTask</tt> is merged into
 * the {@link Job}'s <tt>TaskGraph</tt> subsequent to the <tt>MetaTask</tt>'s
 * execution.
 * 
 * This provides a basic facility for adding {@link Task}s dynamically to a <tt>TaskGraph</tt> during job execution.
 */
public interface MetaTask extends Task {

    /**
     * Get the {@link TaskGraph} created by this {@link MetaTask}.
     * 
     * The returned {@link TaskGraph} will be merged into the {@link Job}'s <tt>TaskGraph</tt> as a successor to this
     * {@link MetaTask}. All edges
     * inbound to the <tt>FINISH</tt> node of the returned <tt>TaskGraph</tt> will be rewired to point to all the
     * successor nodes of this {@link MetaTask}.
     * 
     * The getTaskGraph() method will called right after {@link Task#execute()} had complete.
     * 
     */
    TaskGraph getTaskGraph();

}
