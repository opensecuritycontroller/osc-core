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
package org.osc.core.broker.service.archive;

import org.hibernate.Session;
import org.osc.core.broker.model.entities.archive.JobsArchive;
import org.osc.core.broker.service.ServiceDispatcher;
import org.osc.core.broker.service.request.Request;
import org.osc.core.broker.service.response.BaseDtoResponse;

public class GetJobsArchiveService extends ServiceDispatcher<Request, BaseDtoResponse<JobsArchiveDto>> {

    @Override
    public BaseDtoResponse<JobsArchiveDto> exec(Request request, Session session) throws Exception {
        JobsArchive jobsArchive = (JobsArchive) session.get(JobsArchive.class, 1L);

        BaseDtoResponse<JobsArchiveDto> jobArchiveResponse = new BaseDtoResponse<JobsArchiveDto>();
        jobArchiveResponse.setDto(new JobsArchiveDto());
        jobArchiveResponse.getDto().setId(jobsArchive.getId());
        jobArchiveResponse.getDto().setAutoSchedule(jobsArchive.getAutoSchedule());
        jobArchiveResponse.getDto().setFrequency(jobsArchive.getFrequency());
        jobArchiveResponse.getDto().setThresholdUnit(jobsArchive.getThresholdUnit());
        jobArchiveResponse.getDto().setThresholdValue(jobsArchive.getThresholdValue());
        jobArchiveResponse.getDto().setLastTriggerTimestamp(jobsArchive.getLastTriggerTimestamp());

        return jobArchiveResponse;
    }

}
