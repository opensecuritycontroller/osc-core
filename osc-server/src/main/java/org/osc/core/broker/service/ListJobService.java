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

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.osc.core.broker.model.entities.job.JobRecord;
import org.osc.core.broker.service.dto.JobRecordDto;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.persistence.JobEntityManager;
import org.osc.core.broker.service.request.ListJobRequest;
import org.osc.core.broker.service.response.ListResponse;

public class ListJobService extends ServiceDispatcher<ListJobRequest, ListResponse<JobRecordDto>> {

    ListResponse<JobRecordDto> response = new ListResponse<JobRecordDto>();

    @Override
    public ListResponse<JobRecordDto> exec(ListJobRequest request, Session session) throws Exception {
        // Initializing Entity Manager
        EntityManager<JobRecord> emgr = new EntityManager<JobRecord>(JobRecord.class, session);
        // to do mapping

        List<JobRecordDto> dtoList = new ArrayList<JobRecordDto>();

        final Order[] o = { Order.desc("id") };
        // mapping all the job objects to job dto objects
        for (JobRecord j : emgr.listAll(o)) {
            JobRecordDto dto = new JobRecordDto();
            JobEntityManager.fromEntity(j, dto);
            dtoList.add(dto);
        }

        this.response.setList(dtoList);
        return this.response;
    }

}
