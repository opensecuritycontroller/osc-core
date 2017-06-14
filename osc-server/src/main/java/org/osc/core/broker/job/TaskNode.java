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

import java.lang.reflect.Field;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Future;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.osc.core.broker.job.Job.TaskChangeListener;
import org.osc.core.broker.model.entities.job.TaskRecord;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.util.SessionUtil;
import org.osc.core.broker.util.StaticRegistry;
import org.osc.core.broker.util.db.HibernateUtil;
import org.osc.core.common.job.TaskGuard;
import org.osc.core.common.job.TaskState;
import org.osc.core.common.job.TaskStatus;
import org.osc.sdk.manager.element.TaskElement;
import org.osc.sdk.manager.element.TaskStateElement;
import org.osc.sdk.manager.element.TaskStatusElement;
import org.osgi.service.transaction.control.ScopedWorkException;
import org.osgi.service.transaction.control.TransactionControl;

/**
 *
 * TaskNode is responsible for containing an executable {@link Task} and
 * managing all aspects of task execution and life cycle.
 */
public class TaskNode implements Runnable, TaskElement {

    private static Logger log = Logger.getLogger(TaskNode.class);

    private Task task;

    private TaskGuard taskGaurd;
    private Throwable failReason;
    private TaskState state;
    private TaskStatus status;

    private DateTime queuedTimestamp;
    private DateTime startedTimestamp;
    private DateTime completedTimestamp;

    private TaskGraph taskGraph;

    private HashMap<String, Object> taskOutputs = new HashMap<String, Object>();

    private TaskRecord taskRecord;
    private Set<TaskNode> children = new HashSet<TaskNode>();
    private TaskNode parent;

    public Future<?> future;

    TaskNode(TaskGraph taskGraph, Task task, TaskGuard taskGaurd) {
        this.task = task;
        this.taskGraph = taskGraph;
        this.taskGaurd = taskGaurd;

        this.state = TaskState.NOT_RUNNING;
        this.status = TaskStatus.PASSED;
    }

    TaskNode(TaskGraph taskGraph, TaskNode taskNode) {
        this.task = taskNode.getTask();
        this.taskGraph = taskGraph;
        this.taskGaurd = taskNode.taskGaurd;

        this.state = TaskState.NOT_RUNNING;
        this.status = TaskStatus.PASSED;

        this.parent = taskNode.getProducer();
        this.children = taskNode.getChildren();
    }

    public Task getTask() {
        return this.task;
    }

    synchronized void setState(TaskState state) {
        if (this.state.equals(state)) {
            return;
        }

        this.state = state;

        DateTime now = new DateTime();
        switch (state) {
        case COMPLETED:
            setCompletedTimestamp(now);
            break;
        case PENDING:
            break;
        case QUEUED:
            setQueuedTimestamp(now);
            break;
        case RUNNING:
            setStartedTimestamp(now);
            break;
        default:
            break;
        }

        if (this.taskRecord != null) {
            persistState();
        }

        // Notify all task state change listeners
        if (!isStartOrEndTask() && !this.taskGraph.getJob().getTaskChangeStateListeners().isEmpty()) {
            Thread thread = new Thread(new NotifyTaskChangeStateListeners(this));
            thread.start();
        }
    }

    private void persistState() {
        try {
            EntityManager em = HibernateUtil.getTransactionalEntityManager();
            TransactionControl txControl = HibernateUtil.getTransactionControl();
            // Use a new transaction to persist this update come what may
            txControl.requiresNew(() -> {
                this.taskRecord = em.find(TaskRecord.class, this.taskRecord.getId(),
                        LockModeType.PESSIMISTIC_WRITE);

                this.taskRecord.setState(this.state);
                this.taskRecord.setCompletedTimestamp(safeDate(getCompletedTimestamp()));
                this.taskRecord.setQueuedTimestamp(safeDate(getQueuedTimestamp()));
                this.taskRecord.setStartedTimestamp(safeDate(getStartedTimestamp()));

                this.taskRecord.setName(getSafeTaskName());

                OSCEntityManager.update(em, this.taskRecord, StaticRegistry.transactionalBroadcastUtil());
                return null;
            });
        } catch (ScopedWorkException e) {
            // Unwrap the ScopedWorkException to get the cause from
            // the scoped work (i.e. the executeTransaction() call.
            log.error("Fail to update TaskRecord " + this, e.getCause());
        } catch (Exception e) {
            // TODO: nbartlex - remove when EM and TX are injected
            log.error("Fail to update TaskRecord " + this, e);
        }
    }

    public String getSafeTaskName() {
        // If there an issue with the task producing a name,
        // we want to mask it so we can continue processing but task
        // will reflect the bad name and will get sufficed up.
        String taskName = null;
        try {
            taskName = getTask().getName();
        } catch (Exception ex) {
            log.error("Fail to get task name for TaskRecord " + this, ex);
            taskName = "** Fail to generate Task Name **";
        }
        return taskName;
    }

    private Date safeDate(DateTime dateTime) {
        return dateTime == null ? null : dateTime.toDate();
    }

    void setStatus(TaskStatus status) {
        this.setStatus(status, null);
    }

    synchronized void setStatus(TaskStatus status, Throwable reason) {
        if (this.status.equals(status)) {
            return;
        }

        this.status = status;
        this.failReason = reason;

        if (this.taskRecord != null) {
            persistStatus();
        }

        // Notify all task state change listeners
        if (!isStartOrEndTask() && !this.taskGraph.getJob().getTaskChangeStateListeners().isEmpty()) {
            Thread thread = new Thread(new NotifyTaskChangeStateListeners(this));
            thread.start();
        }
    }

    private class NotifyTaskChangeStateListeners implements Runnable {
        private final TaskNode taskNode;

        public NotifyTaskChangeStateListeners(TaskNode taskNode) {
            this.taskNode = taskNode;
        }

        @Override
        public void run() {
            for (final TaskChangeListener listener : this.taskNode.taskGraph.getJob().getTaskChangeStateListeners()) {
                listener.taskChanged(this.taskNode);
            }
        }
    }

    private void persistStatus() {
        try {
            EntityManager em = HibernateUtil.getTransactionalEntityManager();
            TransactionControl txControl = HibernateUtil.getTransactionControl();
            // Use a new transaction to persist this update come what may
            txControl.requiresNew(() -> {
                this.taskRecord = em.find(TaskRecord.class, this.taskRecord.getId(),
                        LockModeType.PESSIMISTIC_WRITE);

                this.taskRecord.setStatus(this.status);
                if (this.failReason != null) {
                    if (this.failReason.getMessage() != null) {
                        this.taskRecord.setFailReason(this.failReason.getMessage());
                    } else {
                        this.taskRecord.setFailReason(this.failReason.toString());
                    }
                }
                OSCEntityManager.update(em, this.taskRecord, StaticRegistry.transactionalBroadcastUtil());
                return null;
            });
        } catch (ScopedWorkException e) {
            // Unwrap the ScopedWorkException to get the cause from
            // the scoped work (i.e. the executeTransaction() call.
            log.error("Fail to update TaskRecord " + this, e.getCause());
        } catch (Exception e) {
            // TODO: nbartlex - remove when EM and TX are injected
            log.error("Fail to update TaskRecord " + this, e);
        }
    }

    @Override
    public TaskStatusElement getStatus() {
        return new TaskStatusElementImpl(this.status);
    }

    @Override
    public TaskStateElement getState() {
        return new TaskStateElementImpl(this.state);
    }

    public DateTime getCompletedTimestamp() {
        return this.completedTimestamp;
    }

    void setCompletedTimestamp(DateTime completedTimestamp) {
        this.completedTimestamp = completedTimestamp;
    }

    public Set<TaskNode> getSuccessors() {
        return this.taskGraph.getGraph().getSuccessors(this);
    }

    public Set<TaskNode> getPredecessors() {
        return this.taskGraph.getGraph().getPredecessors(this);
    }

    public Set<TaskNode> getAncestors() {
        return this.taskGraph.getGraph().getAncestors(this);
    }

    void setTaskGaurd(TaskGuard taskGaurd) {
        this.taskGaurd = taskGaurd;
    }

    public TaskGuard getTaskGaurd() {
        return this.taskGaurd;
    }

    @Override
    public synchronized void run() {

        try {

            // Set persistence user in context
            if (this.taskRecord != null) {
                SessionUtil.getInstance().setUser(this.taskRecord.getCreatedBy());
            }

            this.setStatus(TaskStatus.PASSED);
            setState(TaskState.RUNNING);

            // Scan all inputs for possible values
            setTaskInputs();

            try {

                log.debug("Executing: " + getTask().getName() + " (Job: " + this.taskGraph.getJob().getName() + ")");
                this.task.execute();

                if (this.task instanceof MetaTask) {
                    this.taskGraph.getJob().mergeMetaTaskTaskGraph(this);
                }

                saveTaskOutputs();
            } catch (Exception e) {

                log.warn("Task " + this + " Failed", e);
                this.setStatus(TaskStatus.FAILED, e);

            } catch (Throwable t) {

                log.fatal("Task " + this + " Failed", t);
                this.setStatus(TaskStatus.FAILED, t);
            }

            setState(TaskState.COMPLETED);

            /*
             * Mark task completed by adding it to completed queue to allow further
             * processing.
             */
            try {
                this.taskGraph.getJob().getPendingCompletedTasksQueue().put(this);
            } catch (InterruptedException e) {
                log.warn("Task " + this + " was interrupted.", e);
            }

        } catch (Throwable t) {
            log.fatal("Fatal error during task execution (" + this + ")", t);
        }
    }

    @Override
    public Throwable getFailReason() {
        return this.failReason;
    }

    private void setTaskInputs() {
        Field[] fields = null;
        try {
            fields = Class.forName(this.task.getClass().getName()).getFields();
        } catch (ClassNotFoundException e) {
            log.error("Fail to retrieve class fields for Task " + this + ".", e);
            return;
        }
        for (Field field : fields) {
            TaskInput taskInput = field.getAnnotation(TaskInput.class);
            if (taskInput != null) {
                // Scan all predecessors for existing output with this name
                Object value = searchPredecessorsOutputs(this, field.getName());
                if (value != null) {
                    try {
                        field.set(this.task, value);
                    } catch (IllegalArgumentException e) {
                        log.error("Fail to set task field member's value fields for Task " + this + ".", e);
                    } catch (IllegalAccessException e) {
                        log.error("Fail to set task field member's value fields for Task " + this + ".", e);
                    }
                }
            }
        }
    }

    private void saveTaskOutputs() throws ClassNotFoundException, IllegalAccessException {
        Field[] fields = Class.forName(this.task.getClass().getName()).getFields();
        for (Field field : fields) {
            TaskOutput taskOutput = field.getAnnotation(TaskOutput.class);
            if (taskOutput != null) {
                this.taskOutputs.put(field.getName(), field.get(this.task));
            }
        }
    }

    private Object searchPredecessorsOutputs(TaskNode taskNode, String outputFieldName) {
        // First look at immediate predecessors
        for (TaskNode predecessorTaskNode : taskNode.getPredecessors()) {
            for (String output : predecessorTaskNode.taskOutputs.keySet()) {
                if (output.equals(outputFieldName)) {
                    return predecessorTaskNode.taskOutputs.get(outputFieldName);
                }
            }
        }
        // If we have not found any, walk up the graph
        for (TaskNode predecessorTaskNode : taskNode.getPredecessors()) {
            Object val = searchPredecessorsOutputs(predecessorTaskNode, outputFieldName);
            if (val != null) {
                return val;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "TaskNode [task=" + this.task + ", state=" + this.state + ", status=" + this.status + ", guard=" + this.taskGaurd + "]";
    }

    public DateTime getStartedTimestamp() {
        return this.startedTimestamp;
    }

    void setStartedTimestamp(DateTime startedTimestamp) {
        this.startedTimestamp = startedTimestamp;
    }

    public DateTime getQueuedTimestamp() {
        return this.queuedTimestamp;
    }

    void setQueuedTimestamp(DateTime queuedTimestamp) {
        this.queuedTimestamp = queuedTimestamp;
    }

    void setTaskStore(TaskRecord taskRecord) {
        this.taskRecord = taskRecord;
    }

    @Override
    public Long getId() {
        return this.taskRecord != null ? this.taskRecord.getId() : null;
    }

    TaskRecord getTaskStore() {
        return this.taskRecord;
    }

    public boolean isStartOrEndTask() {
        return equals(this.taskGraph.getStartTaskNode()) || equals(this.taskGraph.getEndTaskNode());
    }

    @Override
    public Job getJob() {
        return this.taskGraph.getJob();
    }

    TaskRecord getTaskRecord() {
        return this.taskRecord;
    }

    @Override
    public String getName() {
        return getTask().getName();
    }

    public void setParent(TaskNode parent) {
        this.parent = parent;
        parent.addChild(this);
    }

    public TaskNode getProducer() {
        return this.parent;
    }

    public void addChild(TaskNode child) {
        this.children.add(child);
    }

    public Set<TaskNode> getChildren() {
        return this.children;
    }
}
