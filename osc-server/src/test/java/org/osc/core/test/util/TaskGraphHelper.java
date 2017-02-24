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
package org.osc.core.test.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import org.junit.Assert;
import org.osc.core.broker.job.Graph;
import org.osc.core.broker.job.MetaTask;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.job.TaskNode;

public class TaskGraphHelper {
    public static void validateTaskGraph(MetaTask task, TaskGraph expectedTaskGraph) {
        TaskGraph taskGraph = task.getTaskGraph();
        validateTaskGraph(expectedTaskGraph, taskGraph);
    }

    public static void validateTaskGraph(TaskGraph expectedTaskGraph, TaskGraph providedTaskGraph) {
        Assert.assertEquals(
                String.format(
                        "The count of tasks in the graph was different than expected. Expected Graph: %s. Provided Graph: %s.",
                        expectedTaskGraph.toString(), providedTaskGraph.toString()),
                expectedTaskGraph.getTaskCount(), providedTaskGraph.getTaskCount());

        Graph<TaskNode> graph = providedTaskGraph.getGraph();
        Graph<TaskNode> expectedGraph = expectedTaskGraph.getGraph();

        Iterator<TaskNode> graphIterator = new TestTaskNodePreorderIterator(providedTaskGraph.getStartTaskNode(), graph);
        Iterator<TaskNode> expectedGraphIterator = new TestTaskNodePreorderIterator(expectedTaskGraph.getStartTaskNode(), expectedGraph);

        while (graphIterator.hasNext() && expectedGraphIterator.hasNext()) {
            TaskNode tn = graphIterator.next();
            TaskNode etn = expectedGraphIterator.next();

            try {
                TaskNodeComparer.compare(etn, tn);
            } catch (Exception e) {
                Assert.fail(e.getMessage() + "\nReturned Graph:\n" + providedTaskGraph);
            }
        }
    }

    /**
     * Test iterator for the <@link TaskNode> present in <@link Graph>.
     * It will iterate through the graph in preorder deterministically: sorting the successors before visiting the next node.
     */
    private static class TestTaskNodePreorderIterator implements Iterator<TaskNode> {

        private Graph<TaskNode> graph;
        private Stack<TaskNode> nodesToVisit;
        private Set<TaskNode> nodesAlreadyVisited;

        public TestTaskNodePreorderIterator(TaskNode startNode, Graph<TaskNode> graph) {
            this.graph = graph;
            this.nodesToVisit = new Stack<TaskNode>();
            this.nodesAlreadyVisited = new HashSet<TaskNode>();

            this.nodesToVisit.push(startNode);
        }

        @Override
        public boolean hasNext() {
            return !this.nodesToVisit.isEmpty();
        }

        @Override
        public TaskNode next() {
            TaskNode node = this.nodesToVisit.pop();
            this.nodesAlreadyVisited.add(node);

            List<TaskNode> successors = new ArrayList<TaskNode>(this.graph.getSuccessors(node));
            List<TestTaskNode> testSuccessors = toTestTaskNodeList(successors);
            Collections.sort(testSuccessors);
            successors = toTaskNodeList(testSuccessors);

            for (TaskNode successor : successors) {
                if (!this.nodesAlreadyVisited.contains(successor)) {
                    this.nodesToVisit.push(successor);
                }
            }

            return node;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        private class TestTaskNode implements Comparable<TestTaskNode> {
            private TaskNode node;

            TestTaskNode(TaskNode node) {
                this.node = node;
            }

            @Override
            public int compareTo(TestTaskNode other) {
                int result = this.getTaskNode().getTask().getName().compareTo(other.getTaskNode().getTask().getName());

                if (result != 0) {
                    return result;
                }

                result = this.getTaskNode().getTaskGaurd().compareTo(other.getTaskNode().getTaskGaurd());
                return result;
            }

            public TaskNode getTaskNode() {
                return this.node;
            }
        }

        private List<TestTaskNode> toTestTaskNodeList(List<TaskNode> taskNodes) {
            List<TestTaskNode> result = new ArrayList<TestTaskNode>();
            for (TaskNode taskNode: taskNodes) {
                result.add(new TestTaskNode(taskNode));
            }

            return result;
        }

        private List<TaskNode> toTaskNodeList(List<TestTaskNode> testTaskNodes) {
            List<TaskNode> result = new ArrayList<TaskNode>();
            for (TestTaskNode testTaskNode: testTaskNodes) {
                result.add(testTaskNode.getTaskNode());
            }

            return result;
        }
    }
}
