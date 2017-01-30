package org.osc.core.broker.service;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.service.dto.DistributedApplianceInstanceDto;
import org.osc.core.broker.service.persistence.DistributedApplianceInstanceEntityMgr;
import org.osc.core.broker.service.request.BaseIdRequest;
import org.osc.core.broker.service.response.ListResponse;

public class ListDistributedApplianceInstanceByVSService extends
        ServiceDispatcher<BaseIdRequest, ListResponse<DistributedApplianceInstanceDto>> {
    ListResponse<DistributedApplianceInstanceDto> response = new ListResponse<DistributedApplianceInstanceDto>();

    @Override
    public ListResponse<DistributedApplianceInstanceDto> exec(BaseIdRequest request, Session session) throws Exception {
        List<DistributedApplianceInstanceDto> dtoList = new ArrayList<DistributedApplianceInstanceDto>();
        for (DistributedApplianceInstance dai : DistributedApplianceInstanceEntityMgr.listByVsId(session,
                request.getId())) {
            DistributedApplianceInstanceDto dto = new DistributedApplianceInstanceDto(dai);
            dtoList.add(dto);
        }
        this.response.setList(dtoList);
        return this.response;
    }

}
