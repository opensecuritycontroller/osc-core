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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.osc.core.broker.job.Job.JobCompletionListener;
import org.osc.core.broker.job.Job.TaskChangeListener;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.service.common.VmidcMessages;
import org.osc.core.broker.service.common.VmidcMessages_;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

/**
 *
 * JobEngine is a service capable of processing multiple {@link Job}s
 * represented as {@link TaskGraph}.
 */
public final class JobEngine {

    public static final int DEFAULT_JOB_THREAD_POOL_SIZE = 10;
    public static final int DEFAULT_TASK_THREAD_POOL_SIZE = 40;

    private static int jobThreadPoolSize = DEFAULT_JOB_THREAD_POOL_SIZE;
    private static int taskThreadPoolSize = DEFAULT_TASK_THREAD_POOL_SIZE;

    private static Logger logger = Logger.getLogger(JobEngine.class);

    private static JobEngine jobEngine = new JobEngine();
    List<Job> activeJobs = new ArrayList<Job>();
    private boolean initialized = false;
    private boolean isShutdown = false;

    /*
     * The reason we have separate thread pools - one for jobs, one for tasks is
     * if we had only one, an overwhelming requests for new jobs can fill up
     * pool and there will not be any threads left for task execution which will
     * lead to a deadlock.
     */
    private ThreadPoolExecutor jobExecutor = null;
    private ThreadPoolExecutor taskExecutor = null;

    private Set<JobCompletionListener> jobCompletionListeners = new HashSet<JobCompletionListener>();

    private JobEngine() {
    }

    public synchronized void init(int jobThreadPoolSize, int taskThreadPoolSize) {
        if (this.initialized) {
            return;
        }

        this.jobExecutor = new ThreadPoolExecutor(jobThreadPoolSize, jobThreadPoolSize, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(), new ThreadFactoryBuilder().setNameFormat("isc-job-pool-%d")
                        .build(), new RejectedExecutionHandlerImpl());

        this.taskExecutor = new ThreadPoolExecutor(taskThreadPoolSize, taskThreadPoolSize, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(), new ThreadFactoryBuilder().setNameFormat("isc-task-pool-%d")
                        .build(), new RejectedExecutionHandlerImpl());

        jobEngine.initialized = true;
    }

    public static synchronized JobEngine getEngine() {
        if (!jobEngine.initialized) {
            jobEngine.init(jobThreadPoolSize, taskThreadPoolSize);
        }
        return jobEngine;
    }

    public class RejectedExecutionHandlerImpl implements RejectedExecutionHandler {

        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            logger.warn(r.toString() + " is rejected");
        }
    }

    public void shutdown() {
        logger.info("Job Engine Shutdown Requested");
        this.isShutdown = true;
        abortAllJobs(VmidcMessages.getString(VmidcMessages_.JOB_ABORT_SHUTDOWN));
        this.jobExecutor.shutdownNow();
        this.taskExecutor.shutdownNow();
        try {
            logStatus();
            this.taskExecutor.awaitTermination(1, TimeUnit.MINUTES);
            this.jobExecutor.awaitTermination(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            logger.warn("Couldn't wait any longer for job(s) completion. Shutting down job engine.", e);
        }
    }

    /**
     * Shuts down the job engine and allows for reintialization of job engine for future tests.
     */
    void shutdownForTest() {
        shutdown();
        this.initialized = false;
        this.isShutdown = false;
    }

    public void logStatus() {
        logger.debug(String
                .format("[job monitor] [Pool:%d/Core:%d] Active: %d, Completed: %d, Tasks: %d, queus-size: %d, isShutdown: %s, isTerminated: %s",
                        this.jobExecutor.getPoolSize(), this.jobExecutor.getCorePoolSize(),
                        this.jobExecutor.getActiveCount(), this.jobExecutor.getCompletedTaskCount(),
                        this.jobExecutor.getTaskCount(), this.jobExecutor.getQueue().size(),
                        this.jobExecutor.isShutdown(), this.jobExecutor.isTerminated()));
        logger.debug(String
                .format("[task monitor] [Pool:%d/Core:%d] Active: %d, Completed: %d, Tasks: %d, queus-size: %d, isShutdown: %s, isTerminated: %s",
                        this.taskExecutor.getPoolSize(), this.taskExecutor.getCorePoolSize(),
                        this.taskExecutor.getActiveCount(), this.taskExecutor.getCompletedTaskCount(),
                        this.taskExecutor.getTaskCount(), this.taskExecutor.getQueue().size(),
                        this.taskExecutor.isShutdown(), this.taskExecutor.isTerminated()));
    }

    public Job submit(String name, TaskGraph taskGraph, JobCompletionListener listener, boolean persistent)
            throws Exception {
        return initJob(name, taskGraph, null, listener, null, persistent);
    }

    public Job submit(String name, TaskGraph taskGraph, JobCompletionListener jobListener,
            TaskChangeListener taskListener, boolean persistent) throws Exception {
        return initJob(name, taskGraph, null, jobListener, taskListener, persistent);
    }

    public Job submit(String name, TaskGraph taskGraph, boolean persistent) throws Exception {
        return initJob(name, taskGraph, null, null, null, persistent);
    }

    public Job submit(String name, TaskGraph taskGraph, Set<LockObjectReference> objects, JobCompletionListener listener)
            throws Exception {
        return initJob(name, taskGraph, objects, listener, null, true);
    }

    public Job submit(String name, TaskGraph taskGraph, Set<LockObjectReference> objects,
            JobCompletionListener jobListener, TaskChangeListener taskListener) throws Exception {
        return initJob(name, taskGraph, objects, jobListener, taskListener, true);
    }

    public Job submit(String name, TaskGraph taskGraph, Set<LockObjectReference> objects) throws Exception {
        return initJob(name, taskGraph, objects, null, null, true);
    }

    public Job submit(String name, TaskGraph taskGraph, Set<LockObjectReference> objects,
            JobCompletionListener jobCompletionListener, TaskChangeListener taskChangeListener, boolean persistent)
            throws Exception {
        return initJob(name, taskGraph, objects, jobCompletionListener, taskChangeListener, persistent);
    }

    private synchronized Job initJob(String name, TaskGraph taskGraph, Set<LockObjectReference> objects,
            JobCompletionListener jobCompletionListener, TaskChangeListener taskChangeListener, boolean persistent)
            throws Exception {
        if (!this.isShutdown) {
            Job job = new Job(name, taskGraph, objects, this.taskExecutor);
            if (persistent) {
                job.persistJob();
            }
            if (jobCompletionListener != null) {
                job.addListener(jobCompletionListener);
            }
            for (JobCompletionListener listener : this.jobCompletionListeners) {
                job.addListener(listener);
            }

            if (taskChangeListener != null) {
                job.addListener(taskChangeListener);
            }

            job.setState(JobState.QUEUED);
            if (this.jobExecutor.getActiveCount() >= DEFAULT_JOB_THREAD_POOL_SIZE) {
                // We have no threads to execute this job, see if this job has a lock which the
                // other jobs are waiting on, and abort if necessary.
                List<LockObjectReference> currentJobLockReferences = job.getCurrentLockReferences();
                List<LockObjectReference> activeJobsLockReferences = new ArrayList<>();
                Iterator<Job> it = this.activeJobs.iterator();
                while (it.hasNext()) {
                    Job activeJob = it.next();
                    activeJobsLockReferences.addAll(activeJob.getCurrentLockReferences());
                }

                for (LockObjectReference currentJobLockReference : currentJobLockReferences) {
                    if (activeJobsLockReferences.contains(currentJobLockReference)) {
                        job.abort(VmidcMessages.getString(VmidcMessages_.JOB_ABORT_DEADLOCK));
                        throw new VmidcBrokerValidationException("Job will Result in DeadLock. Please try again later.");
                    }
                }
            }
            this.activeJobs.add(job);

            Future<?> future = this.jobExecutor.submit(job);
            job.future = future;
            return job;
        } else {
            throw new VmidcBrokerValidationException("Cannot process job, job engine is shutting down.");
        }
    }

    public boolean isActive() {
        return this.jobExecutor.getActiveCount() > 0;
    }

    public static void setJobThreadPoolSize(String value) {
        if (value == null) {
            return;
        }
        jobThreadPoolSize = Integer.parseInt(value);
    }

    public static void setTaskThreadPoolSize(String value) {
        if (value == null) {
            return;
        }
        taskThreadPoolSize = Integer.parseInt(value);
    }

    private synchronized void abortAllJobs(String reason) {
        // Active jobs will be changing as jobs are completed. Act on snapshot of jobs that we know.
        CopyOnWriteArrayList<Job> activeJobsCopy = new CopyOnWriteArrayList<>(this.activeJobs);
        Iterator<Job> it = activeJobsCopy.iterator();
        while (it.hasNext()) {
            Job job = it.next();
            job.abort(reason);
        }
    }

    public synchronized void abortJob(Long jobId, String reason) {
        Iterator<Job> it = this.activeJobs.iterator();
        while (it.hasNext()) {
            Job job = it.next();
            if (job.getId().equals(jobId)) {
                if (!job.getState().isTerminalState()) {
                    job.abort(reason);
                }
                return;
            }
        }
    }

    public void addJobCompletionListener(JobCompletionListener listener) {
        this.jobCompletionListeners.add(listener);
    }

    public void removeJobCompletionListener(JobCompletionListener listener) {
        this.jobCompletionListeners.remove(listener);
    }

    public synchronized Job getJobByTask(Task task) {
        CopyOnWriteArrayList<Job> activeJobsCopy = new CopyOnWriteArrayList<>(this.activeJobs);
        Iterator<Job> it = activeJobsCopy.iterator();
        while (it.hasNext()) {
            Job job = it.next();
            for (TaskNode taskNode : job.getTaskGraph().getGraph().getNodes()) {
                if (taskNode.getTask() == task) {
                    return job;
                }
            }
        }
        return null;
    }
}
