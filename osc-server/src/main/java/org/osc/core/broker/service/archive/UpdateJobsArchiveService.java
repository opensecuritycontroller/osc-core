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
package org.osc.core.broker.service.archive;

import org.hibernate.Session;
import org.osc.core.broker.model.entities.archive.JobsArchive;
import org.osc.core.broker.service.ServiceDispatcher;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.persistence.JobsArchiveEntityMgr;
import org.osc.core.broker.service.request.BaseRequest;
import org.osc.core.broker.service.response.BaseResponse;

public class UpdateJobsArchiveService extends ServiceDispatcher<BaseRequest<JobsArchiveDto>, BaseResponse> {

    @Override
    public BaseResponse exec(BaseRequest<JobsArchiveDto> request, Session session) throws Exception {
        EntityManager<JobsArchive> emgr = new EntityManager<JobsArchive>(JobsArchive.class, session);

        // retrieve existing entry from db
        JobsArchive jobsArchive = emgr.findByPrimaryKey(request.getDto().getId());

        validate(request, jobsArchive);

        JobsArchiveEntityMgr.toEntity(jobsArchive, request.getDto());
        emgr.update(jobsArchive);
        return new BaseResponse();
    }

    void validate(BaseRequest<JobsArchiveDto> req, JobsArchive jobsArchive) throws Exception {

        if (jobsArchive == null) {
            throw new VmidcBrokerValidationException("Jobs Archive configuration not found.");
        }
        if (req.getDto().getAutoSchedule() == null) {
            throw new VmidcBrokerValidationException("Jobs Archive scheduling cannot be empty.");
        }
        if (req.getDto().getFrequency() == null) {
            throw new VmidcBrokerValidationException("Jobs Archive frequancy cannot be empty.");
        }
        if (req.getDto().getThresholdUnit() == null) {
            throw new VmidcBrokerValidationException("Jobs Archive threshold unit type cannot be empty.");
        }
        if (req.getDto().getThresholdValue() == null || jobsArchive.getThresholdValue() <= 0L) {
            throw new VmidcBrokerValidationException("Jobs Archive threshold value must be a positive number.");
        }
    }

}
