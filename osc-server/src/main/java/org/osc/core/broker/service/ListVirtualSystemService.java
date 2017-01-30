package org.osc.core.broker.service;

import java.util.List;

import org.hibernate.Session;
import org.osc.core.broker.service.dto.VirtualSystemDto;
import org.osc.core.broker.service.persistence.VirtualSystemEntityMgr;
import org.osc.core.broker.service.request.ListVirtualSystemRequest;
import org.osc.core.broker.service.response.ListResponse;

public class ListVirtualSystemService extends ServiceDispatcher<ListVirtualSystemRequest, ListResponse<VirtualSystemDto>> {

    ListResponse<VirtualSystemDto> response = new ListResponse<VirtualSystemDto>();

    @Override
    public ListResponse<VirtualSystemDto> exec(ListVirtualSystemRequest request, Session session) {

        long mcId = request.getMcId();
        long applianceId = request.getApplianceId();
        String applianceSwVer = request.getApplianceSoftwareVersionName();

        List<VirtualSystemDto> ls = VirtualSystemEntityMgr.findByMcApplianceAndSwVer(session, mcId, applianceId, applianceSwVer);
        this.response.setList(ls);

        return this.response;

    }

}
