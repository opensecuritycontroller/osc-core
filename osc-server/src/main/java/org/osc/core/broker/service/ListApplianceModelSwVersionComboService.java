package org.osc.core.broker.service;

import java.util.List;

import org.hibernate.Session;
import org.osc.core.broker.model.plugin.manager.ManagerType;
import org.osc.core.broker.service.dto.ApplianceModelSoftwareVersionDto;
import org.osc.core.broker.service.persistence.ApplianceSoftwareVersionEntityMgr;
import org.osc.core.broker.service.request.ListApplianceModelSwVersionComboRequest;
import org.osc.core.broker.service.response.ListResponse;

public class ListApplianceModelSwVersionComboService extends
        ServiceDispatcher<ListApplianceModelSwVersionComboRequest, ListResponse<ApplianceModelSoftwareVersionDto>> {

    ListResponse<ApplianceModelSoftwareVersionDto> response = new ListResponse<ApplianceModelSoftwareVersionDto>();

    @Override
    public ListResponse<ApplianceModelSoftwareVersionDto> exec(ListApplianceModelSwVersionComboRequest request,
            Session session) {

        ManagerType mcType = request.getType();

        List<ApplianceModelSoftwareVersionDto> ls = ApplianceSoftwareVersionEntityMgr.findByMcType(session, mcType);
        this.response.setList(ls);

        return this.response;

    }

}
