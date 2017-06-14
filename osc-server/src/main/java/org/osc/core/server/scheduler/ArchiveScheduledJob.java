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

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.job.lock.LockObjectReference.ObjectType;
import org.osc.core.broker.model.entities.archive.FreqType;
import org.osc.core.broker.model.entities.events.SystemFailureType;
import org.osc.core.broker.service.api.ArchiveServiceApi;
import org.osc.core.broker.service.api.GetJobsArchiveServiceApi;
import org.osc.core.broker.service.dto.JobsArchiveDto;
import org.osc.core.broker.service.request.BaseRequest;
import org.osc.core.broker.service.request.Request;
import org.osc.core.broker.service.response.BaseDtoResponse;
import org.osc.core.broker.util.StaticRegistry;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class ArchiveScheduledJob implements Job {

    private static final Logger log = Logger.getLogger(ArchiveScheduledJob.class);
    public static final int ARCHIVE_STARTUP_DELAY_IN_MIN = 15;
    public static final String ARCHIVE_JOB_NAME = "archiveJobName";
    public static final String ARCHIVE_GROUP_NAME = "archiveGroupName";

    public ArchiveScheduledJob() {

    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        GetJobsArchiveServiceApi jobsArchiveService = (GetJobsArchiveServiceApi) context.getMergedJobDataMap().get(GetJobsArchiveServiceApi.class.getName());
        ArchiveServiceApi archiveService = (ArchiveServiceApi) context.getMergedJobDataMap().get(ArchiveServiceApi.class.getName());

        try {
            BaseDtoResponse<JobsArchiveDto> reponse = jobsArchiveService.dispatch(new Request() {
            });

            Period period = new Period();
            if (FreqType.valueOf(reponse.getDto().getFrequency()) == FreqType.MONTHLY) {
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
                archiveService.dispatch(request);
            }
        } catch (Exception e) {
            log.error("Failure during archive operation", e);
            StaticRegistry.alertGenerator().processSystemFailureEvent(SystemFailureType.ARCHIVE_FAILURE,
                    new LockObjectReference(1L, "Archive Settings", ObjectType.ARCHIVE),
                    "Failure during archive operation " + e.getMessage());
        }
    }
}
