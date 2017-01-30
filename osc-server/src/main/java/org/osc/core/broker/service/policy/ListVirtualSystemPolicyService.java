package org.osc.core.broker.service.policy;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.management.Policy;
import org.osc.core.broker.service.ServiceDispatcher;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.PolicyEntityMgr;
import org.osc.core.broker.service.persistence.VirtualSystemEntityMgr;
import org.osc.core.broker.service.request.BaseIdRequest;
import org.osc.core.broker.service.response.ListResponse;

public class ListVirtualSystemPolicyService extends ServiceDispatcher<BaseIdRequest, ListResponse<PolicyDto>> {

    private VirtualSystem vs;

    @Override
    public ListResponse<PolicyDto> exec(BaseIdRequest daIdRequest, Session session) throws Exception {

        validate(session, daIdRequest);

        // to do mapping
        List<PolicyDto> dtoList = new ArrayList<PolicyDto>();

        for(Policy policy : this.vs.getDomain().getPolicies()) {
            PolicyDto dto = new PolicyDto();
            PolicyEntityMgr.fromEntity(policy, dto);

            dtoList.add(dto);
        }

        ListResponse<PolicyDto> response = new ListResponse<PolicyDto>();
        response.setList(dtoList);

        return response;
    }

    protected void validate(Session session, BaseIdRequest request) throws Exception {

        this.vs = VirtualSystemEntityMgr.findById(session, request.getId());

        if (this.vs == null) {
            throw new VmidcBrokerValidationException(
                    "Virtual System with Id: " + request.getId() + "  is not found.");
        }

    }

}
