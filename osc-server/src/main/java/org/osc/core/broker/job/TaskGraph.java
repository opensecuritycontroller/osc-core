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
package org.osc.core.broker.job;

import java.util.Set;

/**
 *         TaskGraph contains a tree {@link Graph} of {@link Task}s which will
 *         execute in accordance to dependency order. Tasks added with
 *         dependency must ensure there are no circular loop dependency in the
 *         graph (A->B, B->A)
 */
public class TaskGraph {

    private Graph<TaskNode> graph;
    private TaskNode startTaskNode;
    private TaskNode endTaskNode;
    private Job job;

    public Job getJob() {
        return this.job;
    }

    public TaskGraph() {
        this.startTaskNode = new TaskNode(this, new StartTask(), TaskGuard.ALL_PREDECESSORS_COMPLETED);
        this.endTaskNode = new TaskNode(this, new EndTask(), TaskGuard.ALL_PREDECESSORS_COMPLETED);
        this.graph = new Graph<TaskNode>();

        this.graph.addNode(this.startTaskNode);
        this.graph.addNode(this.endTaskNode);
        this.graph.addEdge(this.startTaskNode, this.endTaskNode);
    }

    /**
     * Add to task graph with no dependency, thus practically creating a
     * parallel branch in task graph.
     *
     * @param task
     *            The task to add.
     */
    public void addTask(Task task) {
        addTask(task, TaskGuard.ALL_PREDECESSORS_SUCCEEDED, this.startTaskNode.getTask());
    }

    /**
     * Add task to task graph with a dependent task(s), thus practically
     * creating a sequential dependency to one or more predecessor(s).
     * Predecessor tasks must be in terminal state which is not necessary
     * successful.
     *
     * @param task
     *            The task to add.
     * @param predecessors
     *            List of one or more predecessor tasks that must complete
     *            successfully before task starts execution.
     */
    public void addTask(Task task, Task... predecessors) {
        addTask(task, TaskGuard.ALL_PREDECESSORS_SUCCEEDED, predecessors);
    }

    /**
     * Add task to task graph with a dependent task(s), thus practically
     * creating a sequential dependency to one or more predecessor(s). Task
     * execution will start only when meeting predecessor tasks guard.
     *
     * @param task
     *            The task to add.
     * @param taskGuard
     *            Task guard condition predecessors task must meet before
     *            execution starts.
     * @param predecessors
     *            List of one or more predecessor tasks that must complete
     *            before task starts execution.
     */
    public synchronized void addTask(Task task, TaskGuard taskGuard, Task... predecessors) {
        TaskNode taskNode = new TaskNode(this, task, taskGuard);
        this.graph.addNode(taskNode);

        if (predecessors.length == 0) {
            predecessors = new Task[1];
            predecessors[0] = this.startTaskNode.getTask();
        }

        /*
         * Add task while setting predecessor
         */
        for (Task predecessorTask : predecessors) {
            TaskNode predecessorTaskNode = getTaskNode(predecessorTask);
            this.graph.addEdge(predecessorTaskNode, taskNode);
            this.graph.addEdge(taskNode, this.endTaskNode);

            if (predecessorTaskNode.getSuccessors().contains(this.endTaskNode)) {
                this.graph.removeEdge(predecessorTaskNode, this.endTaskNode);
            }
        }

    }

    /**
     * Return {@link TaskNode} for a give task.
     *
     * @param task
     *            The task for which owning {@link TaskNode} is being returned.
     * @return {@link TaskNode} containing the task.
     */
    public TaskNode getTaskNode(Task task) {
        for (TaskNode node : this.graph.getNodes()) {
            if (node.getTask().equals(task)) {
                return node;
            }
        }
        return null;
    }

    /**
     * Add task as last task in task graph. All leaf tasks will automatically
     * becomes this task predecessor(s). Conditional execution task guard is set
     * to all predecessor successful completion.
     *
     * @param task
     *            The task to be chained at end of task graph.
     */
    public void appendTask(Task task) {
        appendTask(task, TaskGuard.ALL_PREDECESSORS_SUCCEEDED);
    }

    /**
     * Add task as last task in task graph. All leaf tasks will automatically
     * becomes this task predecessor(s). Conditional execution task guard is set
     * to all predecessor completion.
     *
     * @param task
     *            The task to be chained at end of task graph.
     * @param taskGuard
     *            The task guard predecessors must meet before executing the
     *            wired task.
     */
    public synchronized void appendTask(Task task, TaskGuard taskGuard) {
        // Add task with current leafs as predecessors
        TaskNode tasknode = new TaskNode(this, task, taskGuard);
        this.graph.addEdges(this.graph.getPredecessors(this.endTaskNode), tasknode);

        // Remove end node's predecessor as they are now wired as predecessor to
        // the newly added task
        for (TaskNode predecessor : this.graph.getPredecessors(this.endTaskNode)) {
            this.graph.removeEdge(predecessor, this.endTaskNode);
        }

        this.graph.addEdge(tasknode, this.endTaskNode);
    }

    /**
     * Merge a {@link TaskGraph} to this TaskGraph as a new parallel sequence.
     *
     * @param taskGraph
     *            The TaskGraph to merge
     */
    public void addTaskGraph(TaskGraph taskGraph) {
        addTaskGraph(taskGraph, TaskGuard.ALL_PREDECESSORS_COMPLETED, this.startTaskNode.getTask());
    }

    /**
     * Merge a {@link TaskGraph} to this TaskGraph after a 'latch-on'
     * predecessor task with a default successful completion task guard.
     *
     * @param taskGraph
     *            The TaskGraph to merge.
     * @param predecessor
     *            The {@link Task} after which the merge taskGraph will be
     *            executed.
     */
    public void addTaskGraph(TaskGraph taskGraph, Task predecessor) {
        addTaskGraph(taskGraph, TaskGuard.ALL_PREDECESSORS_SUCCEEDED, predecessor);
    }

    /**
     * Merge a {@link TaskGraph} to this TaskGraph after a 'latch-on'
     * predecessor task with a specific task guard setting.
     *
     * @param taskGraph
     *            The TaskGraph to merge.
     * @param taskGaurd
     *            The conditional task guard setting use to determine if
     *            taskGraph will execute.
     * @param predecessor
     *            The {@link Task} after which the merge taskGraph will be
     *            executed.
     */
    public synchronized void addTaskGraph(TaskGraph taskGraph, TaskGuard taskGaurd, Task predecessor) {

        // Optimization - not need to do anything if taskgraph is empty.
        if (taskGraph.isEmpty()) {
            return;
        }

        TaskNode predecessorTaskNode = getTaskNode(predecessor);

        copyTaskGraphNodesAndEdges(taskGraph);

        // Set latch-on task as predecessor to imported tasks
        boolean taskAdded = false;
        for (TaskNode node : taskGraph.getStartTaskNode().getSuccessors()) {
            // Skip start and end
            if (node.isStartOrEndTask()) {
                continue;
            }
            TaskNode n = getTaskNode(node.getTask());
            if (n != null) {
                this.graph.addEdge(predecessorTaskNode, n);
                n.setTaskGaurd(taskGaurd);
                taskAdded = true;
            }
        }
        if (taskAdded) {
            // If latch-on task happens to be last, remove edge to end task
            this.graph.removeEdge(predecessorTaskNode, getEndTaskNode());
        }

        // Set this graph EndTask as successor to imported leaf tasks
        for (TaskNode node : taskGraph.getEndTaskNode().getPredecessors()) {
            // Skip start and end
            if (node.isStartOrEndTask()) {
                continue;
            }
            TaskNode n = getTaskNode(node.getTask());
            if (n != null) {
                this.graph.addEdge(n, getEndTaskNode());
            }
        }
    }

    private void copyTaskGraphNodesAndEdges(TaskGraph taskGraph) {
        // Add duplicated copy of all nodes with the exception of start/end
        // tasks
        for (TaskNode node : taskGraph.getGraph().getNodes()) {
            // Skip start and end
            if (node.isStartOrEndTask()) {
                continue;
            }
            this.graph.addNode(new TaskNode(this, node));
        }

        // Duplicate dependencies with the exception of references to start/end
        // tasks
        for (TaskNode node : taskGraph.getGraph().getNodes()) {
            // Skip start and end
            if (node.isStartOrEndTask()) {
                continue;
            }

            TaskNode nd = getTaskNode(node.getTask());
            for (TaskNode pre : node.getPredecessors()) {
                // Skip start and end
                if (pre.isStartOrEndTask()) {
                    continue;
                }
                TaskNode ns = getTaskNode(pre.getTask());
                this.graph.addEdge(ns, nd);
            }
        }
    }

    public void insertTaskGraph(TaskGraph insertedTaskGraph) {
        insertTaskGraph(insertedTaskGraph, this.startTaskNode.getTask());
    }

    public synchronized void insertTaskGraph(TaskGraph insertedTaskGraph, Task predecessor) {

        // Optimization - not need to do anything if inserted taskgraph is empty.
        if (insertedTaskGraph.isEmpty()) {
            return;
        }

        TaskNode predecessorTaskNode = getTaskNode(predecessor);
        Set<TaskNode> successorsBeforeMerge = predecessorTaskNode.getSuccessors();

        this.addTaskGraph(insertedTaskGraph, predecessorTaskNode.getTask());

        // Add leaf tasks as predecessors to meta task's successors
        for (TaskNode node : insertedTaskGraph.getEndTaskNode().getPredecessors()) {
            // Skip start and end
            if (node.isStartOrEndTask()) {
                continue;
            }
            TaskNode n = getTaskNode(node.getTask());
            if (n != null) {
                /*
                 * addTaskGraph() ties all newly added leaf tasks to end task.
                 * Need to remove that link as leafs will be predecessors to
                 * meta task's successors. We must do this first in case meta
                 * task successor(s) is the end task. Otherwise, leafs would not
                 * be grounded.
                 */
                getGraph().removeEdge(n, getEndTaskNode());
                /*
                 * Set leafs as predecessors to meta task successors
                 */
                for (TaskNode s : successorsBeforeMerge) {
                    getGraph().addEdge(n, s);
                    getGraph().removeEdge(predecessorTaskNode, s);
                }
            }
        }

    }

    /**
     * Returns the graph holding task nodes for purpose of traversal.
     *
     * @return Graph of {@link }TaskNode}s
     */
    public Graph<TaskNode> getGraph() {
        return this.graph;
    }

    /**
     * Return special start task which exist in each task graph.
     *
     * @return The first {@link Task} of the {@link TaskGraph}
     */
    public TaskNode getStartTaskNode() {
        return this.startTaskNode;
    }

    /**
     * Return special end task which exist in each task graph.
     *
     * @return The last {@link Task} of the {@link TaskGraph}
     */
    public TaskNode getEndTaskNode() {
        return this.endTaskNode;
    }

    @Override
    public String toString() {
        return "TaskGraph [\n" + this.graph + "\n]";
    }

    void setJob(Job job) {
        this.job = job;
    }

    public int getTaskCount() {
        return this.graph.getNodes().size() - 2 /* ignore start/end tasks */;
    }

    public boolean isEmpty() {
        return getTaskCount() == 0;
    }
}
