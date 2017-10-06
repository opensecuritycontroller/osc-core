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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.osc.core.broker.job.Job.JobCompletionListener;
import org.osc.core.broker.job.Job.TaskChangeListener;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.util.log.LogProvider;
import org.slf4j.Logger;

/**
 * Queues the jobs submitted so that they can be submitted in the order of invocation to the job engine.
 */
public final class JobQueuer {

    private static final BlockingQueue<JobRequest> jobQueue = new LinkedBlockingQueue<>();
    private static JobQueuer queuer = null;

    private static Logger log = LogProvider.getLogger(JobQueuer.class);

    public static class JobRequest {
        private String name;
        private TaskGraph taskGraph;
        private Set<LockObjectReference> objects;
        private JobCompletionListener jobCompletionListener;
        private TaskChangeListener taskChangeListener;
        private boolean persistent;

        public JobRequest(String name, TaskGraph taskGraph, Set<LockObjectReference> objects,
                JobCompletionListener jobCompletionListener, TaskChangeListener taskChangeListener, boolean persistent) {
            this.name = name;
            this.taskGraph = taskGraph;
            this.objects = objects;
            this.jobCompletionListener = jobCompletionListener;
            this.taskChangeListener = taskChangeListener;
            this.persistent = persistent;
        }

        public JobRequest(String name, TaskGraph taskGraph, JobCompletionListener listener, boolean persistent) {
            this(name, taskGraph, null, listener, null, persistent);
        }

        public JobRequest(String name, TaskGraph taskGraph, JobCompletionListener jobListener,
                TaskChangeListener taskListener, boolean persistent) {
            this(name, taskGraph, null, jobListener, taskListener, persistent);
        }

        public JobRequest(String name, TaskGraph taskGraph, boolean persistent) {
            this(name, taskGraph, null, null, null, persistent);
        }

        public JobRequest(String name, TaskGraph taskGraph, Set<LockObjectReference> objects,
                JobCompletionListener listener) {
            this(name, taskGraph, objects, listener, null, true);
        }

        public JobRequest(String name, TaskGraph taskGraph, Set<LockObjectReference> objects,
                JobCompletionListener jobListener, TaskChangeListener taskListener) {
            this(name, taskGraph, objects, jobListener, taskListener, true);
        }

        public JobRequest(String name, TaskGraph taskGraph, Set<LockObjectReference> objects) {
            this(name, taskGraph, objects, null, null, true);
        }

        public String getName() {
            return this.name;
        }

        public TaskGraph getTaskGraph() {
            return this.taskGraph;
        }

        public Set<LockObjectReference> getObjects() {
            return this.objects;
        }

        public JobCompletionListener getJobCompletionListener() {
            return this.jobCompletionListener;
        }

        public TaskChangeListener getTaskChangeListener() {
            return this.taskChangeListener;
        }

        public boolean isPersistent() {
            return this.persistent;
        }
    }

    private JobQueuer() {
        Thread queueThread = new Thread("JobQueuer-Thread") {

            @Override
            public void run() {
                while (true) {
                    try {
                        JobRequest jobToExecute = jobQueue.take();

                        Job submittedJob = JobEngine.getEngine().submit(jobToExecute.getName(),
                                jobToExecute.getTaskGraph(), jobToExecute.getObjects(),
                                jobToExecute.getJobCompletionListener(), jobToExecute.getTaskChangeListener(),
                                jobToExecute.isPersistent());
                        log.info(String.format("Submitted Job: '%s'(%s)", jobToExecute.getName(), submittedJob.getId()));
                        submittedJob.waitForCompletion();
                        log.info(String.format("Submitted Job Completed: '%s'(%s)", jobToExecute.getName(),
                                submittedJob.getId()));
                    } catch (Exception e) {
                        log.error("Exception while processing jobs in queue.", e);
                    }
                }
            }
        };
        queueThread.start();
    }

    public static synchronized JobQueuer getInstance() {
        if (queuer == null) {
            queuer = new JobQueuer();
        }
        return queuer;
    }

    /**
     * Adds the Job to the job queue so it can be executed after any previously queued jobs have
     * been executed.
     */
    public synchronized void putJob(JobRequest job) {
        try {
            log.info(String.format("Job %s put into queue", job.getName()));
            jobQueue.put(job);
        } catch (InterruptedException e) {
            log.warn("Putting job in queue was interrupted.", e);
        }
    }
}
