package org.osc.core.broker.service;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.osc.core.broker.model.entities.appliance.ApplianceSoftwareVersion;
import org.osc.core.broker.service.dto.ApplianceSoftwareVersionDto;
import org.osc.core.broker.service.persistence.ApplianceSoftwareVersionEntityMgr;
import org.osc.core.broker.service.request.ListApplianceSoftwareVersionByApplianceIdRequest;
import org.osc.core.broker.service.response.ListResponse;



public class ListApplianceSoftwareVersionByApplianceIdService extends
        ServiceDispatcher<ListApplianceSoftwareVersionByApplianceIdRequest, ListResponse<ApplianceSoftwareVersionDto>> {

    @Override
    public ListResponse<ApplianceSoftwareVersionDto> exec(ListApplianceSoftwareVersionByApplianceIdRequest request,
            Session session) {

        List<ApplianceSoftwareVersion> ls = ApplianceSoftwareVersionEntityMgr
                .getApplianceSoftwareVersionsByApplianceId(session, request.getApplianceId());
        List<ApplianceSoftwareVersionDto> dtoList = new ArrayList<ApplianceSoftwareVersionDto>();

        ListResponse<ApplianceSoftwareVersionDto> response = new ListResponse<ApplianceSoftwareVersionDto>();

        if (ls != null) {
            // mapping all the appliance sw ver objects to appliance sw ver dto
            // objects
            for (ApplianceSoftwareVersion av : ls) {

                ApplianceSoftwareVersionDto dto = new ApplianceSoftwareVersionDto();

                ApplianceSoftwareVersionEntityMgr.fromEntity(av, dto);

                dtoList.add(dto);

            }

            response.setList(dtoList);
        }

        return response;
    }

}
