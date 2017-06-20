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

import javax.persistence.EntityManager;

import org.osc.core.broker.model.entities.archive.JobsArchive;
import org.osc.core.broker.service.ServiceDispatcher;
import org.osc.core.broker.service.api.GetJobsArchiveServiceApi;
import org.osc.core.broker.service.dto.JobsArchiveDto;
import org.osc.core.broker.service.request.Request;
import org.osc.core.broker.service.response.BaseDtoResponse;
import org.osgi.service.component.annotations.Component;

@Component
public class GetJobsArchiveService extends ServiceDispatcher<Request, BaseDtoResponse<JobsArchiveDto>>
        implements GetJobsArchiveServiceApi {

    @Override
    public BaseDtoResponse<JobsArchiveDto> exec(Request request, EntityManager em) throws Exception {
        JobsArchive jobsArchive = em.find(JobsArchive.class, 1L);

        BaseDtoResponse<JobsArchiveDto> jobArchiveResponse = new BaseDtoResponse<JobsArchiveDto>();
        jobArchiveResponse.setDto(new JobsArchiveDto());
        jobArchiveResponse.getDto().setId(jobsArchive.getId());
        jobArchiveResponse.getDto().setAutoSchedule(jobsArchive.getAutoSchedule());
        jobArchiveResponse.getDto().setFrequency(jobsArchive.getFrequency().toString());
        jobArchiveResponse.getDto().setThresholdUnit(jobsArchive.getThresholdUnit().toString());
        jobArchiveResponse.getDto().setThresholdValue(jobsArchive.getThresholdValue());
        jobArchiveResponse.getDto().setLastTriggerTimestamp(jobsArchive.getLastTriggerTimestamp());

        return jobArchiveResponse;
    }

}
