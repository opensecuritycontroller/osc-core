/*******************************************************************************
 * Copyright (c) Intel Corporation
 * Copyright (c) 2017
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.osc.core.broker.service.policy;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;

import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.model.entities.management.Policy;
import org.osc.core.broker.service.ServiceDispatcher;
import org.osc.core.broker.service.dto.PolicyDto;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.ApplianceManagerConnectorEntityMgr;
import org.osc.core.broker.service.persistence.PolicyEntityMgr;
import org.osc.core.broker.service.request.BaseIdRequest;
import org.osc.core.broker.service.response.ListResponse;

public class ListManagerConnectoryPolicyService extends ServiceDispatcher<BaseIdRequest, ListResponse<PolicyDto>> {

    private ApplianceManagerConnector mc;

    @Override
    public ListResponse<PolicyDto> exec(BaseIdRequest daIdRequest, EntityManager em) throws Exception {

        validate(em, daIdRequest);

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

    protected void validate(EntityManager em, BaseIdRequest request) throws Exception {

        this.mc = ApplianceManagerConnectorEntityMgr.findById(em, request.getId());

        if (this.mc == null) {
            throw new VmidcBrokerValidationException("Manager Connector with Id: " + request.getId() + "  is not found.");
        }

    }

}
