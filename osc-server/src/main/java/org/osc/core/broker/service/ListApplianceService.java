package org.osc.core.broker.service;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.osc.core.broker.model.entities.appliance.Appliance;
import org.osc.core.broker.service.dto.ApplianceDto;
import org.osc.core.broker.service.dto.BaseDto;
import org.osc.core.broker.service.persistence.ApplianceEntityMgr;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.request.BaseRequest;
import org.osc.core.broker.service.response.ListResponse;



public class ListApplianceService extends ServiceDispatcher<BaseRequest<BaseDto>, ListResponse<ApplianceDto>> {

    ListResponse<ApplianceDto> response = new ListResponse<ApplianceDto>();

    @Override
    public ListResponse<ApplianceDto> exec(BaseRequest<BaseDto> request, Session session) {
        // Initializing Entity Manager
        EntityManager<Appliance> emgr = new EntityManager<Appliance>(Appliance.class, session);
        // to do mapping
        List<ApplianceDto> dtoList = new ArrayList<ApplianceDto>();

        // mapping all the appliance objects to appliance dto objects
        for (Appliance a : emgr.listAll(new Order[] { Order.asc("model") })) {

            ApplianceDto dto = new ApplianceDto();

            ApplianceEntityMgr.fromEntity(a, dto);

            dtoList.add(dto);
        }

        this.response.setList(dtoList);
        return this.response;
    }

}
