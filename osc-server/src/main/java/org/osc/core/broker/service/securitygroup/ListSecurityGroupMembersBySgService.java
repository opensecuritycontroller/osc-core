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

import java.util.HashSet;
import java.util.Set;

import org.hibernate.Session;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMember;
import org.osc.core.broker.service.ServiceDispatcher;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.SecurityGroupEntityMgr;
import org.osc.core.broker.service.persistence.SecurityGroupMemberEntityMgr;
import org.osc.core.broker.service.request.BaseIdRequest;
import org.osc.core.broker.service.response.SetResponse;

public class ListSecurityGroupMembersBySgService extends
        ServiceDispatcher<BaseIdRequest, SetResponse<SecurityGroupMemberItemDto>> {

    private SecurityGroup sg;

    @Override
    public SetResponse<SecurityGroupMemberItemDto> exec(BaseIdRequest request, Session session) throws Exception {

        validate(session, request);
        // to do mapping
        Set<SecurityGroupMemberItemDto> dtoList = new HashSet<SecurityGroupMemberItemDto>();

        for (SecurityGroupMember securityGroupMember : SecurityGroupMemberEntityMgr
                .listActiveSecurityGroupMembersBySecurityGroup(session, this.sg)) {

            SecurityGroupMemberItemDto dto = new SecurityGroupMemberItemDto();

            SecurityGroupMemberEntityMgr.fromEntity(securityGroupMember, dto);

            dtoList.add(dto);
        }

        return new SetResponse<>(dtoList);
    }

    protected void validate(Session session, BaseIdRequest request) throws Exception {
        BaseIdRequest.checkForNullId(request);

        this.sg = SecurityGroupEntityMgr.findById(session, request.getId());

        if (this.sg == null) {
            throw new VmidcBrokerValidationException("Security Group with Id: " + request.getId() + "  is not found.");
        }
    }
}
