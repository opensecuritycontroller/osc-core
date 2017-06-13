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
package org.osc.core.broker.service;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;

import org.osc.core.broker.job.JobEngine;
import org.osc.core.broker.model.entities.job.JobRecord;
import org.osc.core.broker.service.api.ListJobServiceApi;
import org.osc.core.broker.service.dto.JobRecordDto;
import org.osc.core.broker.service.persistence.JobEntityManager;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.request.ListJobRequest;
import org.osc.core.broker.service.response.ListResponse;
import org.osgi.service.component.annotations.Component;

@Component
public class ListJobService extends ServiceDispatcher<ListJobRequest, ListResponse<JobRecordDto>>
        implements ListJobServiceApi {


    @Override
    public ListResponse<JobRecordDto> exec(ListJobRequest request, EntityManager em) throws Exception {
        ListResponse<JobRecordDto> response = new ListResponse<JobRecordDto>();

        // Initializing Entity Manager
        OSCEntityManager<JobRecord> emgr = new OSCEntityManager<JobRecord>(JobRecord.class, em, this.txBroadcastUtil);
        // to do mapping

        List<JobRecordDto> dtoList = new ArrayList<JobRecordDto>();

        // mapping all the job objects to job dto objects
        for (JobRecord j : emgr.listAll(false, "id")) {
            JobRecordDto dto = new JobRecordDto();
            JobEntityManager.fromEntity(j, dto);
            dtoList.add(dto);
        }

        response.setList(dtoList);
        return response;
    }

    @Override
    public void abortJob(Long jobId, String reason) {
        JobEngine.getEngine().abortJob(jobId, reason);
    }

}
