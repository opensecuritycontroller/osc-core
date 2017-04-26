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
package org.osc.core.server.scheduler;

import java.util.Date;

import org.apache.commons.lang.time.DateUtils;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.job.lock.LockObjectReference.ObjectType;
import org.osc.core.broker.model.entities.archive.FreqType;
import org.osc.core.broker.model.entities.events.SystemFailureType;
import org.osc.core.broker.service.alert.AlertGenerator;
import org.osc.core.broker.service.archive.ArchiveService;
import org.osc.core.broker.service.archive.GetJobsArchiveService;
import org.osc.core.broker.service.dto.JobsArchiveDto;
import org.osc.core.broker.service.request.BaseRequest;
import org.osc.core.broker.service.request.Request;
import org.osc.core.broker.service.response.BaseDtoResponse;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;

public class ArchiveScheduledJob implements Job {

    private static final int ARCHIVE_STARTUP_DELAY_IN_MIN = 15;

    private static final Logger log = Logger.getLogger(ArchiveScheduledJob.class);

    private static final String ARCHIVE_JOB_NAME = "archiveJobName";
    private static final String ARCHIVE_GROUP_NAME = "archiveGroupName";

    public ArchiveScheduledJob() {

    }

    /**
     * Schedule the archiving job based on the interval defined in the DB.
     *
     * If archiving is disabled, deletes the archiving job.
     */
    public static void maybeScheduleArchiveJob() {

        GetJobsArchiveService service = new GetJobsArchiveService();
        try {
            BaseDtoResponse<JobsArchiveDto> reponse = service.dispatch(new Request() {
            });

            Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
            boolean doesJobExists = scheduler.checkExists(new JobKey(ARCHIVE_JOB_NAME, ARCHIVE_GROUP_NAME));

            if (reponse.getDto().getAutoSchedule() && !doesJobExists) {
                JobDetail archiveJob = JobBuilder.newJob(ArchiveScheduledJob.class)
                        .withIdentity(ARCHIVE_JOB_NAME, ARCHIVE_GROUP_NAME).build();

                Trigger archiveTrigger = TriggerBuilder
                        .newTrigger()
                        .startAt(DateUtils.addMinutes(new Date(), ARCHIVE_STARTUP_DELAY_IN_MIN))
                        .withSchedule(
                                SimpleScheduleBuilder.simpleSchedule().withIntervalInHours(24).repeatForever()
                                        .withMisfireHandlingInstructionFireNow()).build();

                scheduler.scheduleJob(archiveJob, archiveTrigger);

                log.info("Archiving job check is scheduled to run every 24 hours. Starting at: "
                        + archiveTrigger.getStartTime());

            } else if (!reponse.getDto().getAutoSchedule()) {
                // If archiving is disabled, delete the job if it exists
                if (scheduler.deleteJob(new JobKey(ARCHIVE_JOB_NAME, ARCHIVE_GROUP_NAME))) {
                    log.info("Archiving job Deleted");
                }
            }
        } catch (Exception e) {
            log.error("Scheduler fail to start/stop Archiving job", e);
            AlertGenerator.processSystemFailureEvent(SystemFailureType.ARCHIVE_FAILURE,
                    new LockObjectReference(1L, "Archive Settings", ObjectType.ARCHIVE),
                    "Failure during archive schedule configuration " + e.getMessage());
        }
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            GetJobsArchiveService service = new GetJobsArchiveService();
            BaseDtoResponse<JobsArchiveDto> reponse = service.dispatch(new Request() {
            });

            Period period = new Period();
            if (reponse.getDto().getFrequency().equals(FreqType.MONTHLY)) {
                period = Period.months(1);
            } else {
                period = Period.weeks(1);
            }
            DateTime futureJob = new DateTime(reponse.getDto().getLastTriggerTimestamp()).plus(period);

            if (reponse.getDto().getLastTriggerTimestamp() == null || futureJob.isBeforeNow()) {
                log.info("Archiving Job Trigggered at : " + new Date());
                BaseRequest<JobsArchiveDto> request = new BaseRequest<JobsArchiveDto>();
                request.setDto(new JobsArchiveDto());
                request.getDto().setId(1L);
                new ArchiveService().dispatch(request);
            }
        } catch (Exception e) {
            log.error("Failure during archive operation", e);
            AlertGenerator.processSystemFailureEvent(SystemFailureType.ARCHIVE_FAILURE,
                    new LockObjectReference(1L, "Archive Settings", ObjectType.ARCHIVE),
                    "Failure during archive operation " + e.getMessage());
        }
    }

}
