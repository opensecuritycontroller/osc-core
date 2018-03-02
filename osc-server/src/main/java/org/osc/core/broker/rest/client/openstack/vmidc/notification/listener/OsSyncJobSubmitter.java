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

package org.osc.core.broker.rest.client.openstack.vmidc.notification.listener;

import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;

import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.service.SecurityGroupConformJobFactory;
import org.osc.core.broker.service.exceptions.VmidcBrokerInvalidRequestException;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OsSyncJobSubmitter {

    // Anything that a Quartz job is going to touch has to be public
    public static final int SG_SYNC_WAIT_SECONDS = 45;
    public static final long DEAD_JOB_TIMEOUT = 7200;
    public static final Map<Long, Instant> submittedJobs = new HashMap<>();
    public static final Logger log = LoggerFactory.getLogger(OsSyncJobSubmitter.class);
    public static Scheduler scheduler;

    public static void triggerSGSync(SecurityGroup sg, EntityManager em,
            SecurityGroupConformJobFactory sgConformJobFactory)
                    throws Exception {

        log.info("Running SG sync based on OS Port notification received.");

        if (decisionToSubmit(sg)) {
            checkScheduler();

            JobDataMap jobDataMap = new JobDataMap();
            jobDataMap.put("sg", sg);
            jobDataMap.put("jobFactory", sgConformJobFactory);
            jobDataMap.put("em", em);

            Trigger trigger = TriggerBuilder.newTrigger()
                    .startAt(Date.from(Instant.now().plusSeconds(SG_SYNC_WAIT_SECONDS))).build();

            JobDetail syncJob = JobBuilder.newJob(SubmitSGSyncJob.class).setJobData(jobDataMap).build();
            scheduler.scheduleJob(syncJob, trigger);
        }
    }

    private static boolean decisionToSubmit(SecurityGroup sg) {

        synchronized (submittedJobs) {
            if (!submittedJobs.containsKey(sg.getId())) {
                submittedJobs.put(sg.getId(), Instant.now());
                return true;
            } else if (submittedJobs.get(sg.getId()).isBefore(Instant.now().minusSeconds(DEAD_JOB_TIMEOUT))) {
                log.warn("Another job is stuck in the queue waiting to synchronize SG {} ", sg.getId());
                submittedJobs.put(sg.getId(), Instant.now());
                return true;
            }
        }

        return false;
    }

    private static void checkScheduler() throws SchedulerException {
        if (scheduler == null || !scheduler.isStarted() || scheduler.isShutdown() || scheduler.isInStandbyMode()) {
            scheduler = StdSchedulerFactory.getDefaultScheduler();
            scheduler.start();
        }
    }

    public static class SubmitSGSyncJob implements org.quartz.Job {
        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
            SecurityGroup sg = (SecurityGroup) context.getMergedJobDataMap().get("sg");
            log.info("Scheduling SG sync job based on OS Port notification. SG {}", sg.getId());

            synchronized (submittedJobs) {
                submittedJobs.remove(sg.getId());
            }

            SecurityGroupConformJobFactory sgConformJobFactory = (SecurityGroupConformJobFactory)
                    context.getMergedJobDataMap().get("jobFactory");

            try {
                sgConformJobFactory.startSecurityGroupConformanceJob(sg);

            } catch (VmidcBrokerInvalidRequestException vbire) {
                log.info("Another sync job is already running for SG {}. Scheduling later.", sg.getId());
                log.info("Another sync job is already running", vbire);

                EntityManager em = (EntityManager) context.getMergedJobDataMap().get("em");

                try {
                    triggerSGSync(sg, em, sgConformJobFactory);
                } catch (Exception e) {
                    String msg = String.format("Failed to trigger SG %s sync after retry.", sg.getId());
                    log.error(msg);
                    throw new JobExecutionException(msg, e);
                }
            } catch (Exception e) {
                String msg = String.format("Exception submitting SG %s sync", sg.getId());
                log.error(msg);
                throw new JobExecutionException(e);
            }
        }
    }
}
