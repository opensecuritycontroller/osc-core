package org.osc.core.broker.service.vc;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.service.ServiceDispatcher;
import org.osc.core.broker.service.dto.BaseDto;
import org.osc.core.broker.service.dto.VirtualizationConnectorDto;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.persistence.VirtualizationConnectorEntityMgr;
import org.osc.core.broker.service.request.BaseRequest;
import org.osc.core.broker.service.response.ListResponse;



public class ListVirtualizationConnectorService extends ServiceDispatcher<BaseRequest<BaseDto>, ListResponse<VirtualizationConnectorDto>> {

    ListResponse<VirtualizationConnectorDto> response = new ListResponse<VirtualizationConnectorDto>();

    @Override
    public ListResponse<VirtualizationConnectorDto> exec(BaseRequest<BaseDto> request, Session session) {
        // Initializing Entity Manager
        EntityManager<VirtualizationConnector> emgr = new EntityManager<VirtualizationConnector>(
                VirtualizationConnector.class, session);
        // to do mapping
        List<VirtualizationConnectorDto> vcmList = new ArrayList<VirtualizationConnectorDto>();

        // mapping all the VC objects to vc dto objects
        for (VirtualizationConnector vc : emgr.listAll(new Order[] { Order.asc("name") })) {
            VirtualizationConnectorDto dto = new VirtualizationConnectorDto();
            VirtualizationConnectorEntityMgr.fromEntity(vc, dto);
            if (request.isApi()) {
                VirtualizationConnectorDto.sanitizeVirtualizationConnector(dto);
            }
            vcmList.add(dto);
        }
        this.response.setList(vcmList);
        return this.response;
    }

}
