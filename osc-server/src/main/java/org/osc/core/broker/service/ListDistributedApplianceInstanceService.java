package org.osc.core.broker.service;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.service.dto.BaseDto;
import org.osc.core.broker.service.dto.DistributedApplianceInstanceDto;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.request.BaseRequest;
import org.osc.core.broker.service.response.ListResponse;

public class ListDistributedApplianceInstanceService extends
ServiceDispatcher<BaseRequest<BaseDto>, ListResponse<DistributedApplianceInstanceDto>> {

    ListResponse<DistributedApplianceInstanceDto> response = new ListResponse<DistributedApplianceInstanceDto>();

    @Override
    public ListResponse<DistributedApplianceInstanceDto> exec(BaseRequest<BaseDto> request, Session session) throws Exception {
        EntityManager<DistributedApplianceInstance> emgr = new EntityManager<DistributedApplianceInstance>(
                DistributedApplianceInstance.class, session);
        List<DistributedApplianceInstanceDto> daiList = new ArrayList<DistributedApplianceInstanceDto>();
        for (DistributedApplianceInstance dai : emgr.listAll(new Order[] { Order.asc("name") })) {
            DistributedApplianceInstanceDto dto = new DistributedApplianceInstanceDto(dai);
            daiList.add(dto);
        }
        this.response.setList(daiList);
        return this.response;
    }
}
