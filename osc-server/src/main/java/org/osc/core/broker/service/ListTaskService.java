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

import org.osc.core.broker.model.entities.job.TaskRecord;
import org.osc.core.broker.service.api.ListTaskServiceApi;
import org.osc.core.broker.service.dto.TaskRecordDto;
import org.osc.core.broker.service.persistence.TaskEntityMgr;
import org.osc.core.broker.service.request.ListTaskRequest;
import org.osc.core.broker.service.response.ListResponse;
import org.osgi.service.component.annotations.Component;

@Component
public class ListTaskService extends ServiceDispatcher<ListTaskRequest, ListResponse<TaskRecordDto>>
        implements ListTaskServiceApi {

    @Override
    public ListResponse<TaskRecordDto> exec(ListTaskRequest request, EntityManager em) throws Exception {

        List<TaskRecordDto> dtoList = new ArrayList<TaskRecordDto>();
        TaskEntityMgr emgr = new TaskEntityMgr(em);

        for (TaskRecord tr : emgr.getTasksByJobId(request.getJobId())) {
            TaskRecordDto dto = new TaskRecordDto();
            TaskEntityMgr.fromEntity(tr, dto);
            dtoList.add(dto);
        }

        ListResponse<TaskRecordDto> response = new ListResponse<TaskRecordDto>();
        response.setList(dtoList);
        return response;
    }
}
