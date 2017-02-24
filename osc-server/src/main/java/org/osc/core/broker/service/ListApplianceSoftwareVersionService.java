package org.osc.core.broker.service;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.osc.core.broker.model.entities.appliance.ApplianceSoftwareVersion;
import org.osc.core.broker.service.dto.ApplianceSoftwareVersionDto;
import org.osc.core.broker.service.persistence.ApplianceSoftwareVersionEntityMgr;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.request.ListApplianceSoftwareVersionRequest;
import org.osc.core.broker.service.response.ListResponse;



public class ListApplianceSoftwareVersionService extends
        ServiceDispatcher<ListApplianceSoftwareVersionRequest, ListResponse<ApplianceSoftwareVersionDto>> {

    @Override
    public ListResponse<ApplianceSoftwareVersionDto> exec(ListApplianceSoftwareVersionRequest request, Session session) {
        // Initializing Entity Manager
        EntityManager<ApplianceSoftwareVersion> emgr = new EntityManager<ApplianceSoftwareVersion>(
                ApplianceSoftwareVersion.class, session);
        // to do mapping
        List<ApplianceSoftwareVersionDto> dtoList = new ArrayList<ApplianceSoftwareVersionDto>();

        // mapping all the av objects to av dto objects
        for (ApplianceSoftwareVersion av : emgr.findByParentId("appliance", request.getApplianceId(),
                new Order[] { Order.asc("applianceSoftwareVersion") })) {

            ApplianceSoftwareVersionDto dto = new ApplianceSoftwareVersionDto();

            ApplianceSoftwareVersionEntityMgr.fromEntity(av, dto);
            dtoList.add(dto);
        }

        ListResponse<ApplianceSoftwareVersionDto> response = new ListResponse<ApplianceSoftwareVersionDto>();
        response.setList(dtoList);
        return response;
    }

}
