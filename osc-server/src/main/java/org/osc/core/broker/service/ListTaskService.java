package org.osc.core.broker.service;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.osc.core.broker.model.entities.job.TaskRecord;
import org.osc.core.broker.service.dto.TaskRecordDto;
import org.osc.core.broker.service.persistence.TaskEntityMgr;
import org.osc.core.broker.service.request.ListTaskRequest;
import org.osc.core.broker.service.response.ListResponse;

public class ListTaskService extends ServiceDispatcher<ListTaskRequest, ListResponse<TaskRecordDto>> {

    @Override
    public ListResponse<TaskRecordDto> exec(ListTaskRequest request, Session session) throws Exception {

        List<TaskRecordDto> dtoList = new ArrayList<TaskRecordDto>();
        TaskEntityMgr emgr = new TaskEntityMgr(session);

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
