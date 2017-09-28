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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;

import org.osc.core.broker.job.lock.LockManager;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.job.lock.LockRequest;
import org.osc.core.broker.model.entities.ObjectType;
import org.osc.core.broker.model.entities.job.JobObject;
import org.osc.core.broker.model.entities.job.JobRecord;
import org.osc.core.broker.model.entities.job.TaskObject;
import org.osc.core.broker.model.entities.job.TaskRecord;
import org.osc.core.broker.service.LockUtil;
import org.osc.core.broker.service.common.VmidcMessages;
import org.osc.core.broker.service.common.VmidcMessages_;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.tasks.conformance.UnlockObjectMetaTask;
import org.osc.core.broker.service.tasks.conformance.UnlockObjectTask;
import org.osc.core.broker.util.SessionUtil;
import org.osc.core.broker.util.StaticRegistry;
import org.osc.core.broker.util.db.HibernateUtil;
import org.osc.core.broker.util.log.LogProvider;
import org.osc.core.common.job.JobState;
import org.osc.core.common.job.JobStatus;
import org.osc.core.common.job.TaskGuard;
import org.osc.core.common.job.TaskState;
import org.osc.core.common.job.TaskStatus;
import org.osc.sdk.manager.element.JobElement;
import org.osgi.service.transaction.control.ScopedWorkException;
import org.osgi.service.transaction.control.TransactionControl;
import org.slf4j.Logger;

/**
 *
 * Job class is returned by the {@link JobEngine} on submission and
 * holds a running job.
 */
public class Job implements Runnable, JobElement {

    private static Logger log = LogProvider.getLogger(Job.class);

    public interface JobCompletionListener {
        void completed(Job job);
    }

    public interface TaskChangeListener {
        void taskChanged(TaskNode taskNode);
    }

    private final String name;
    private final TaskGraph taskGraph;
    private final Set<LockObjectReference> objects;

    private JobState state = JobState.NOT_RUNNING;
    private JobStatus status = JobStatus.PASSED;
    private String failureReason;

    private Date queuedTimestamp;
    private Date startedTimestamp;
    private Date completedTimestamp;
    private boolean isAborted = false;

    private final ThreadPoolExecutor executor;

    private final BlockingQueue<TaskNode> pendingCompletedTasksQueue = new LinkedBlockingQueue<TaskNode>();

    private final Set<JobCompletionListener> jobCompletionListeners = new HashSet<JobCompletionListener>();
    private Set<TaskChangeListener> taskChangeListeners = new HashSet<TaskChangeListener>();

    private final Semaphore jobCompletionSemaphore = new Semaphore(1);

    private JobRecord jobRecord;
    Future<?> future;

    Job(String name, TaskGraph taskGraph, Set<LockObjectReference> objects, ThreadPoolExecutor taskExecutor) {
        this.taskGraph = taskGraph;
        this.name = name;
        this.objects = objects;
        this.executor = taskExecutor;

        taskGraph.setJob(this);

        /*
         * Acquire completion lock on construction is the earliest we can do so
         * before job gets submitted for execution.
         */
        try {
            this.jobCompletionSemaphore.acquire();
        } catch (InterruptedException e) {
            log.warn("Job " + this + " was interrupted.", e);
        }
    }

    @Override
    public JobStatusElementImpl getStatus() {
        return new JobStatusElementImpl(this.status);
    }

    void setStatus(JobStatus status) {
        this.status = status;

        if (this.jobRecord != null) {
            persistStatus();
        }
    }

    private void persistStatus() {

        try {
            EntityManager em = HibernateUtil.getTransactionalEntityManager();
            TransactionControl txControl = HibernateUtil.getTransactionControl();
            // Use a new transaction to persist this update come what may
            txControl.requiresNew(() -> {
                this.jobRecord = em.find(JobRecord.class, this.jobRecord.getId());
                this.jobRecord.setStatus(this.status);
                OSCEntityManager.update(em, this.jobRecord, StaticRegistry.transactionalBroadcastUtil());
                return null;
            });
        } catch (ScopedWorkException e) {
            // Unwrap the ScopedWorkException to get the cause from
            // the scoped work (i.e. the executeTransaction() call.
            log.error("Fail to update JobRecord " + this, e.getCause());
        } catch (Exception e) {
            // TODO:nbartlex remove when EM and TX are injected in A7
            log.error("Fail to update JobRecord " + this, e);
        }
    }

    public Date getCompletedTimestamp() {
        return this.completedTimestamp;
    }

    public String getName() {
        return this.name;
    }

    public TaskGraph getTaskGraph() {
        return this.taskGraph;
    }

    @Override
    public void run() {
        executeGraph(getTaskGraph().getStartTaskNode());
    }

    private void executeTask(TaskNode taskNode) {
        // Task is assumed to be queued till execution starts.
        taskNode.setState(TaskState.QUEUED);

        synchronized (this) {
            Future<?> future = this.executor.submit(taskNode);
            taskNode.future = future;
        }
    }

    private void executeGraph(TaskNode rootTaskNode) {

        setState(JobState.RUNNING);

        // Execute pre-order root task first to ensure execution order
        executeTask(rootTaskNode);
        while (true) {
            try {
                TaskNode completedTask = null;

                try {
                    // Wait for any completed tasks
                    completedTask = this.pendingCompletedTasksQueue.take();
                } catch (InterruptedException e) {
                    log.warn("Job " + this + " was interrupted.", e);
                    break;
                }

                if (this.isAborted) {
                    log.warn("Job " + this + " was cancelled.");
                    break;
                }

                // if one task fails whole job is set to fail
                if (!((TaskStatusElementImpl)completedTask.getStatus()).getStatus().isSuccessful()) {
                    setFailureReason(VmidcMessages.getString(VmidcMessages_.JOB_TASK_FAILURE));
                    setStatus(JobStatus.FAILED);
                }

                /*
                 * If the completed task has no successors, it means it is the last
                 * task (EndTask) and job is completed.
                 */
                if (completedTask.getSuccessors().isEmpty()) {
                    break;
                }

                /*
                 * Check successors and start them if possible (all their
                 * predecessors completed)
                 */
                for (TaskNode successorTaskNode : completedTask.getSuccessors()) {
                    /*
                     * If is running, nothing to do, we'll need to wait for it. If
                     * it is completed, Nothing to do. If is pending a thread,
                     * nothing to do
                     */
                    if (((TaskStateElementImpl)successorTaskNode.getState()).getState().isRunning() || ((TaskStateElementImpl)successorTaskNode.getState()).getState().isTerminalState()
                            || ((TaskStateElementImpl)successorTaskNode.getState()).getState().equals(TaskState.QUEUED)) {
                        continue;
                    }

                    if (checkAllPredecessorsCompleted(successorTaskNode)) {
                        // If task guard require success completion..
                        TaskGuard taskGuard = successorTaskNode.getTaskGaurd();
                        if (taskGuard == TaskGuard.ALL_PREDECESSORS_SUCCEEDED
                                || taskGuard == TaskGuard.ALL_ANCESTORS_SUCCEEDED) {
                            // Check if all predecessor completion state
                            Boolean shouldExecuteTask = false;

                            if (taskGuard == TaskGuard.ALL_PREDECESSORS_SUCCEEDED) {
                                shouldExecuteTask = checkAllPredecessorsCompletedSuccessfully(successorTaskNode);
                            } else if (taskGuard == TaskGuard.ALL_ANCESTORS_SUCCEEDED) {
                                shouldExecuteTask = checkAllAncestorsCompletedSuccessfully(successorTaskNode);
                            }

                            if (shouldExecuteTask) {

                                executeTask(successorTaskNode);
                            } else {
                                /*
                                 * If task does not meet task guard criteria, no need
                                 * to run this task. Just mark it as SKIPPED and add
                                 * it to completed queue so successors still have a
                                 * chance to execute and they may not require
                                 * successful completion.
                                 */
                                markTaskSkipped(successorTaskNode);
                            }

                        } else {

                            executeTask(successorTaskNode);
                        }

                    } else {
                        /*
                         * If not all predecessors had been completed, this task is
                         * marked PENDING. Then loop back to wait for other
                         * predecessors to complete.
                         */
                        successorTaskNode.setState(TaskState.PENDING);
                    }
                }

            } catch (Throwable t) {

                log.error("Fatal error during job execution (" + this + ")", t);
                abort("Fatal error during job execution (" + t + ")");
                break;
            }
        }

        setState(JobState.COMPLETED);

        // Notify all job completion listeners
        if (!this.jobCompletionListeners.isEmpty()) {
            Thread thread = new Thread(new NotifyJobCompletionListeners(this));
            thread.start();
        } else {
            // If anyone's waiting on waitForJobCompletion() call..
            this.jobCompletionSemaphore.release();
        }

        JobEngine.getEngine().activeJobs.remove(this);
    }

    private class NotifyJobCompletionListeners implements Runnable {
        private final Job job;

        public NotifyJobCompletionListeners(Job job) {
            this.job = job;
        }

        @Override
        public void run() {
            for (JobCompletionListener listener : Job.this.jobCompletionListeners) {
                listener.completed(this.job);
            }
            // If anyone's waiting on waitForJobCompletion() call..
            Job.this.jobCompletionSemaphore.release();
        }
    }

    private boolean checkAllPredecessorsCompleted(TaskNode taskNode) {
        // Check if task's predecessor all completed.
        for (TaskNode predecessorTaskNode : taskNode.getPredecessors()) {
            if (!((TaskStateElementImpl)predecessorTaskNode.getState()).getState().isTerminalState()) {
                return false;
            }
        }
        return true;
    }

    private void markTaskSkipped(TaskNode taskNode) {
        taskNode.setState(TaskState.COMPLETED);
        taskNode.setStatus(TaskStatus.SKIPPED);
        try {
            this.pendingCompletedTasksQueue.put(taskNode);
        } catch (InterruptedException e) {
            log.warn("Job " + this + " was interrupted.", e);
        }
    }

    private boolean checkAllPredecessorsCompletedSuccessfully(TaskNode taskNode) {
        boolean isAllPredecessorsSuccessful = true;
        for (TaskNode predecessorTaskNode : taskNode.getPredecessors()) {
            if (!((TaskStatusElementImpl)predecessorTaskNode.getStatus()).getStatus().isSuccessful()) {
                isAllPredecessorsSuccessful = false;
                break;
            }
        }
        return isAllPredecessorsSuccessful;
    }

    private boolean checkAllAncestorsCompletedSuccessfully(TaskNode taskNode) {
        boolean isAllAncestorsSuccessful = true;
        for (TaskNode ancestorTaskNode : taskNode.getAncestors()) {
            if (!((TaskStatusElementImpl)ancestorTaskNode.getStatus()).getStatus().isSuccessful()) {
                isAllAncestorsSuccessful = false;
                break;
            }
        }
        return isAllAncestorsSuccessful;
    }

    BlockingQueue<TaskNode> getPendingCompletedTasksQueue() {
        return this.pendingCompletedTasksQueue;
    }

    @Override
    public JobStateElementImpl getState() {
        return new JobStateElementImpl(this.state);
    }

    void setState(JobState state) {
        this.state = state;

        switch (state) {
        case NOT_RUNNING:
            break;
        case QUEUED:
            this.queuedTimestamp = new Date();
            break;
        case RUNNING:
            this.startedTimestamp = new Date();
            break;
        case COMPLETED:
            this.completedTimestamp = new Date();
            break;
        default:
            break;
        }

        if (this.jobRecord != null) {
            persistState();
        }
    }

    private void persistState() {
        try {
            EntityManager em = HibernateUtil.getTransactionalEntityManager();
            TransactionControl txControl = HibernateUtil.getTransactionControl();
            // Use a new transaction to persist this update come what may
            txControl.requiresNew(() -> {
                this.jobRecord = em.find(JobRecord.class, this.jobRecord.getId());

                this.jobRecord.setState(this.state);
                this.jobRecord.setQueuedTimestamp(getQueuedTimestamp());
                this.jobRecord.setStartedTimestamp(getStartedTimestamp());
                this.jobRecord.setCompletedTimestamp(getCompletedTimestamp());
                this.jobRecord.setFailureReason(getFailureReason());
                OSCEntityManager.update(em, this.jobRecord, StaticRegistry.transactionalBroadcastUtil());
                return null;
            });
        } catch (ScopedWorkException e) {
            // Unwrap the ScopedWorkException to get the cause from
            // the scoped work (i.e. the executeTransaction() call.
            log.error("Fail to update JobRecord " + this, e.getCause());
        } catch (Exception e) {
            // TODO: nbartlex - remove when EM and TX are injected
            log.error("Fail to update JobRecord " + this, e);
        }
    }

    public Date getStartedTimestamp() {
        return this.startedTimestamp;
    }

    public String getFailureReason() {
        return this.failureReason;
    }

    void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
        if (this.jobRecord != null) {
            persistState();
        }
    }

    @Override
    public String toString() {
        return "Job [name=" + this.name + ", state=" + this.state + ", status=" + this.status + "]";
    }

    public void addListener(JobCompletionListener listener) {
        this.jobCompletionListeners.add(listener);
    }

    public void removeListener(JobCompletionListener listener) {
        this.jobCompletionListeners.remove(listener);
    }

    void addListener(TaskChangeListener listener) {
        this.taskChangeListeners.add(listener);
    }

    Set<TaskChangeListener> getTaskChangeStateListeners() {
        return this.taskChangeListeners;
    }

    void setTaskChangeStateListeners(Set<TaskChangeListener> taskChangeListeners) {
        this.taskChangeListeners = taskChangeListeners;
    }

    public void waitForCompletion() {
        try {
            this.jobCompletionSemaphore.acquire();
        } catch (InterruptedException e) {
            log.warn("Job " + this + " was interrupted.", e);
        }
    }

    public Date getQueuedTimestamp() {
        return this.queuedTimestamp;
    }

    void setJobStore(JobRecord jobRecord) {
        this.jobRecord = jobRecord;
    }

    @Override
    public Long getId() {
        return this.jobRecord != null ? this.jobRecord.getId() : null;
    }

    public void abort(String reason) {
        log.info("Abort job " + getId());

        if (this.state.isTerminalState()) {
            log.info("Job " + getId() + " already completed.");
            return;
        }

        this.isAborted = true;
        if (this.future != null) {
            this.future.cancel(true);
        }

        // Mark all tasks running jobs completed/aborted
        for (TaskNode taskNode : this.taskGraph.getGraph().topologicalSort()) {
            if (taskNode.future != null && ((TaskStateElementImpl)taskNode.getState()).getState().isRunning()) {
                taskNode.future.cancel(true);
                taskNode.setStatus(TaskStatus.ABORTED);
                taskNode.setState(TaskState.COMPLETED);
            } else {
                if (!((TaskStateElementImpl)taskNode.getState()).getState().isTerminalState()) {
                    taskNode.setStatus(TaskStatus.SKIPPED);
                    taskNode.setState(TaskState.COMPLETED);
                }
            }
        }

        releaseLocks(this.taskGraph);

        setFailureReason(reason);
        setState(JobState.COMPLETED);
        setStatus(JobStatus.ABORTED);
    }

    JobRecord getJobRecord() {
        return this.jobRecord;
    }

    synchronized void mergeMetaTaskTaskGraph(TaskNode metaTaskNode) throws Exception {
        MetaTask metaTask = (MetaTask) metaTaskNode.getTask();
        TaskGraph tg = metaTask.getTaskGraph();
        if (tg == null || tg.isEmpty()) {
            return;
        }

        for (TaskNode node : tg.getGraph().getNodes()) {
            node.setParent(metaTaskNode);
        }

        this.taskGraph.insertTaskGraph(tg, metaTaskNode.getTask());

        // If task is persisted, need to re-persist calculated order and
        // dependencies
        if (metaTaskNode.getTaskRecord() != null) {
            try {
                persistJob();

            } catch (Exception e) {

                /*
                 * Oh boy, we already added new tasks to task graph but failed
                 * to persist them. Since inserted tasks require predecessor
                 * (meta task itself) to succeed, they won't be executed, unless
                 * of course they are wired to execute even if their predecessor
                 * task fails, such in the case of an unlock task. Regardless,
                 * we don't want to take any change. We'll iterate on newly
                 * added tasks and release any acquired locks.
                 */
                log.error("Fail to persist metatask generated task graph. Releasing locks.", e);
                releaseLocks(tg);

                throw e;
            }
        }
    }

    private void releaseLocks(TaskGraph tg) {
        for (TaskNode task : tg.getGraph().getNodes()) {
            if (task.getTask() instanceof UnlockObjectTask) {
                UnlockObjectTask uot = (UnlockObjectTask) task.getTask();
                LockManager.getLockManager().releaseLock(new LockRequest(uot));
                task.setStatus(TaskStatus.PASSED);
                task.setState(TaskState.COMPLETED);
            } else if (task.getTask() instanceof UnlockObjectMetaTask) {
                UnlockObjectMetaTask uot = (UnlockObjectMetaTask) task.getTask();
                LockUtil.releaseLocks(uot);
                task.setStatus(TaskStatus.PASSED);
                task.setState(TaskState.COMPLETED);
            }
        }
    }

    synchronized void persistJob() throws Exception {
        try {
            EntityManager em = HibernateUtil.getTransactionalEntityManager();
            TransactionControl txControl = HibernateUtil.getTransactionControl();
            // Use a new transaction to persist this Job come what may
            txControl.requiresNew(() -> {
                JobRecord jobRecord = getJobRecord();

                if (jobRecord == null) {
                    String contextUser = SessionUtil.getInstance().getCurrentUser();

                    jobRecord = new JobRecord();
                    jobRecord.setSubmittedBy(contextUser);
                    jobRecord.setName(getName());
                    jobRecord.setState(this.state);
                    jobRecord.setStatus(this.status);

                    jobRecord.setCreatedBy(contextUser);
                    jobRecord.setCreatedTimestamp(new Date());

                    if (this.objects != null) {
                        // Add object references only on creation to ensure uniqueness
                        for (LockObjectReference lor : this.objects) {
                            JobObject jobObject = new JobObject(jobRecord, lor.getName(),
                                    toEntityType(ObjectType.class, lor.getType()), lor.getId());
                            jobRecord.addObject(jobObject);
                        }
                    }

                    OSCEntityManager.create(em, jobRecord, StaticRegistry.transactionalBroadcastUtil());

                    setJobStore(jobRecord);
                }

                persistTaskGraph(em);
                return null;
            });
        } catch (ScopedWorkException e) {
            // Unwrap the ScopedWorkException to get the cause from
            // the scoped work (i.e. the executeTransaction() call.
            throw e.as(Exception.class);
        } catch (Exception e) {
            // TODO: nbartlex - remove when EM and TX are injected
            log.error("Fail to create JobRecord " + this, e);
        }
    }

    static <T extends Enum<T>> T toEntityType(Class<T> toClass, Enum<?> original) {
        return original == null ? null : (T) Enum.valueOf(toClass, original.name());
    }

    private void persistTaskGraph(EntityManager em) {
        Long i = 1L;
        for (TaskNode taskNode : this.taskGraph.getGraph().topologicalSort()) {
            if (taskNode.isStartOrEndTask()) {
                continue;
            }

            TaskRecord taskRecord = taskNode.getTaskStore();
            if (taskRecord == null) {
                taskRecord = new TaskRecord(getJobRecord());
                taskRecord.setCreatedBy(getJobRecord().getCreatedBy());
                taskRecord.setCreatedTimestamp(new Date());
            } else {
                taskRecord = em.find(TaskRecord.class, taskRecord.getId(),
                        LockModeType.PESSIMISTIC_WRITE);
                taskRecord.setUpdatedBy(getJobRecord().getCreatedBy());
                taskRecord.setUpdatedTimestamp(new Date());
            }
            taskNode.setTaskStore(taskRecord);

            taskRecord.setName(taskNode.getSafeTaskName());
            taskRecord.setDependencyOrder(i++);
            taskRecord.setTaskGaurd(taskNode.getTaskGaurd());
            taskRecord.setState(((TaskStateElementImpl)taskNode.getState()).getState());
            taskRecord.setStatus(((TaskStatusElementImpl)taskNode.getStatus()).getStatus());

            if (taskRecord.getId() == null) {
                if (taskNode.getTask().getObjects() != null) {
                    // Add object references only on creation to ensure uniqueness
                    for (LockObjectReference lor : taskNode.getTask().getObjects()) {
                        TaskObject taskObject = new TaskObject(taskRecord,
                                lor.getName(), toEntityType(ObjectType.class, lor.getType()),
                                lor.getId());
                        taskRecord.addObject(taskObject);
                    }
                }

                OSCEntityManager.create(em, taskRecord, StaticRegistry.transactionalBroadcastUtil());
            } else {
                OSCEntityManager.update(em, taskRecord, StaticRegistry.transactionalBroadcastUtil());
            }
        }

        /*
         * Set predecessors/successors
         */
        for (TaskNode taskNode : this.taskGraph.getGraph().topologicalSort()) {
            // Skip persistence of start/end
            if (taskNode.isStartOrEndTask()) {
                continue;
            }

            TaskRecord taskRecord = taskNode.getTaskStore();
            taskRecord = em.find(TaskRecord.class, taskRecord.getId(),
                    LockModeType.PESSIMISTIC_WRITE);

            for (TaskNode taskPredecessor : taskNode.getPredecessors()) {
                // Skip persistence of start/end
                if (taskPredecessor.isStartOrEndTask()) {
                    continue;
                }

                TaskRecord task = taskPredecessor.getTaskStore();
                taskRecord.addPredecessor(task);
            }
            for (TaskNode taskSuccessor : taskNode.getSuccessors()) {
                // Skip persistence of start/end
                if (taskSuccessor.isStartOrEndTask()) {
                    continue;
                }

                TaskRecord task = taskSuccessor.getTaskStore();
                taskRecord.addSuccessor(task);
            }

            if (taskNode.getProducer() != null) {
                taskNode.getProducer().getTaskRecord().addChild(taskRecord);
                OSCEntityManager.update(em, taskNode.getProducer().getTaskRecord(),
                        StaticRegistry.transactionalBroadcastUtil());
            }
        }
    }

    /**
     * Gets all the acquired lock references to objects within the task graph
     *
     */
    List<LockObjectReference> getCurrentLockReferences() {
        List<LockObjectReference> lockReferences = new ArrayList<>();
        Set<TaskNode> allNodes = this.taskGraph.getGraph().getNodes();

        for (TaskNode node : allNodes) {
            Task task = node.getTask();
            if (task instanceof UnlockObjectTask) {
                UnlockObjectTask unlockTask = (UnlockObjectTask) task;
                lockReferences.add(unlockTask.getObjectRef());
            } else if (task instanceof UnlockObjectMetaTask) {
                UnlockObjectMetaTask unlockMetaTask = (UnlockObjectMetaTask) task;
                lockReferences.addAll(unlockMetaTask.getLockObjectReferences());
            }
        }
        return lockReferences;
    }

}
