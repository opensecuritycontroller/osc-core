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
