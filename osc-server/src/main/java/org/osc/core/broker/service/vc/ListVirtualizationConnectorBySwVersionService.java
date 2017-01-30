package org.osc.core.broker.service.vc;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.service.ServiceDispatcher;
import org.osc.core.broker.service.dto.VirtualizationConnectorDto;
import org.osc.core.broker.service.persistence.VirtualizationConnectorEntityMgr;
import org.osc.core.broker.service.response.ListResponse;



public class ListVirtualizationConnectorBySwVersionService extends
        ServiceDispatcher<ListVirtualizationConnectorBySwVersionRequest, ListResponse<VirtualizationConnectorDto>> {

    ListResponse<VirtualizationConnectorDto> response = new ListResponse<VirtualizationConnectorDto>();

    @Override
    public ListResponse<VirtualizationConnectorDto> exec(ListVirtualizationConnectorBySwVersionRequest request,
            Session session) {

        // to do mapping
        List<VirtualizationConnectorDto> vcmList = new ArrayList<VirtualizationConnectorDto>();
        String swVersion = request.getSwVersion();

        // mapping all the VC objects to vc dto objects
        for (VirtualizationConnector vc : VirtualizationConnectorEntityMgr.listBySwVersion(session, swVersion)) {
            VirtualizationConnectorDto dto = new VirtualizationConnectorDto();
            VirtualizationConnectorEntityMgr.fromEntity(vc, dto);
            vcmList.add(dto);
        }
        this.response.setList(vcmList);
        return this.response;
    }

}
