package org.osc.core.broker.service.securityinterface;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupInterface;
import org.osc.core.broker.service.ServiceDispatcher;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.persistence.SecurityGroupInterfaceEntityMgr;
import org.osc.core.broker.service.request.BaseIdRequest;
import org.osc.core.broker.service.response.ListResponse;

public class ListSecurityGroupInterfaceServiceByVirtualSystem extends
        ServiceDispatcher<BaseIdRequest, ListResponse<SecurityGroupInterfaceDto>> {

    private VirtualSystem vs;
    ListResponse<SecurityGroupInterfaceDto> response = new ListResponse<SecurityGroupInterfaceDto>();

    @Override
    public ListResponse<SecurityGroupInterfaceDto> exec(BaseIdRequest request, Session session) throws Exception {
        // to do mapping
        validateAndLoad(request, session);

        List<SecurityGroupInterfaceDto> dtoList = new ArrayList<SecurityGroupInterfaceDto>();

        // Mapping all the da objects to da dto objects
        for (SecurityGroupInterface sgi : this.vs.getSecurityGroupInterfaces()) {
            SecurityGroupInterfaceDto dto = new SecurityGroupInterfaceDto();
            SecurityGroupInterfaceEntityMgr.fromEntity(sgi, dto);
            dtoList.add(dto);
        }

        this.response.setList(dtoList);
        return this.response;
    }

    private void validateAndLoad(BaseIdRequest req, Session session) throws Exception {
        BaseIdRequest.checkForNullId(req);
        EntityManager<VirtualSystem> emgr = new EntityManager<VirtualSystem>(VirtualSystem.class, session);
        this.vs = emgr.findByPrimaryKey(req.getId());
        if (this.vs == null) {
            throw new VmidcBrokerValidationException("Virtual System with Id: " + req.getId() + "  is not found.");
        }

    }

}
