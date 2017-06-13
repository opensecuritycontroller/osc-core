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
package org.osc.core.broker.service.securitygroup;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;

import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.service.ServiceDispatcher;
import org.osc.core.broker.service.api.ListSecurityGroupByVcServiceApi;
import org.osc.core.broker.service.dto.SecurityGroupDto;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.SecurityGroupEntityMgr;
import org.osc.core.broker.service.persistence.VirtualizationConnectorEntityMgr;
import org.osc.core.broker.service.request.BaseIdRequest;
import org.osc.core.broker.service.response.ListResponse;
import org.osc.core.broker.service.validator.BaseIdRequestValidator;
import org.osgi.service.component.annotations.Component;

@Component
public class ListSecurityGroupByVcService extends ServiceDispatcher<BaseIdRequest, ListResponse<SecurityGroupDto>>
        implements ListSecurityGroupByVcServiceApi {


    @Override
    public ListResponse<SecurityGroupDto> exec(BaseIdRequest request, EntityManager em) throws Exception {
        ListResponse<SecurityGroupDto> response = new ListResponse<SecurityGroupDto>();

        validate(em, request);
        // to do mapping
        List<SecurityGroupDto> dtoList = new ArrayList<SecurityGroupDto>();

        for (SecurityGroup securityGroup : SecurityGroupEntityMgr.listSecurityGroupsByVcId(em, request.getId())) {

            SecurityGroupDto dto = new SecurityGroupDto();

            SecurityGroupEntityMgr.fromEntity(securityGroup, dto);
            SecurityGroupEntityMgr.generateDescription(em, dto);

            dtoList.add(dto);
        }

        response.setList(dtoList);
        return response;
    }

    protected void validate(EntityManager em, BaseIdRequest request) throws Exception {
        BaseIdRequestValidator.checkForNullId(request);

        VirtualizationConnector vc = VirtualizationConnectorEntityMgr.findById(em, request.getId());

        if (vc == null) {
            throw new VmidcBrokerValidationException("Virtualization Connector with Id: " + request.getId()
                    + "  is not found.");
        }
    }
}
