package org.osc.core.broker.service.securitygroup;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.service.ServiceDispatcher;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.SecurityGroupEntityMgr;
import org.osc.core.broker.service.persistence.VirtualizationConnectorEntityMgr;
import org.osc.core.broker.service.request.BaseIdRequest;
import org.osc.core.broker.service.response.ListResponse;

public class ListSecurityGroupByVcService extends ServiceDispatcher<BaseIdRequest, ListResponse<SecurityGroupDto>> {

    ListResponse<SecurityGroupDto> response = new ListResponse<SecurityGroupDto>();

    @Override
    public ListResponse<SecurityGroupDto> exec(BaseIdRequest request, Session session) throws Exception {

        validate(session, request);
        // to do mapping
        List<SecurityGroupDto> dtoList = new ArrayList<SecurityGroupDto>();

        for (SecurityGroup securityGroup : SecurityGroupEntityMgr.listSecurityGroupsByVcId(session, request.getId())) {

            SecurityGroupDto dto = new SecurityGroupDto();

            SecurityGroupEntityMgr.fromEntity(securityGroup, dto);
            SecurityGroupEntityMgr.generateDescription(session, dto);

            dtoList.add(dto);
        }

        this.response.setList(dtoList);
        return this.response;
    }

    protected void validate(Session session, BaseIdRequest request) throws Exception {
        BaseIdRequest.checkForNullId(request);

        VirtualizationConnector vc = VirtualizationConnectorEntityMgr.findById(session, request.getId());

        if (vc == null) {
            throw new VmidcBrokerValidationException("Virtualization Connector with Id: " + request.getId()
                    + "  is not found.");
        }
    }
}
