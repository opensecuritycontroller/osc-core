package org.osc.core.broker.service.policy;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.model.entities.management.Policy;
import org.osc.core.broker.service.ServiceDispatcher;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.ApplianceManagerConnectorEntityMgr;
import org.osc.core.broker.service.persistence.PolicyEntityMgr;
import org.osc.core.broker.service.request.BaseIdRequest;
import org.osc.core.broker.service.response.ListResponse;

public class ListManagerConnectoryPolicyService extends ServiceDispatcher<BaseIdRequest, ListResponse<PolicyDto>> {

    private ApplianceManagerConnector mc;

    @Override
    public ListResponse<PolicyDto> exec(BaseIdRequest daIdRequest, Session session) throws Exception {

        validate(session, daIdRequest);

        // to do mapping
        List<PolicyDto> dtoList = new ArrayList<PolicyDto>();

        for (Policy policy : this.mc.getPolicies()) {
            PolicyDto dto = new PolicyDto();
            PolicyEntityMgr.fromEntity(policy, dto);

            dtoList.add(dto);
        }

        ListResponse<PolicyDto> response = new ListResponse<PolicyDto>();
        response.setList(dtoList);

        return response;
    }

    protected void validate(Session session, BaseIdRequest request) throws Exception {

        this.mc = ApplianceManagerConnectorEntityMgr.findById(session, request.getId());

        if (this.mc == null) {
            throw new VmidcBrokerValidationException("Manager Connector with Id: " + request.getId() + "  is not found.");
        }

    }

}
