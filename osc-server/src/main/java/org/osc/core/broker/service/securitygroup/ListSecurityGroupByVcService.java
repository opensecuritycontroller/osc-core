/*******************************************************************************
 * Copyright (c) 2017 Intel Corporation
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
