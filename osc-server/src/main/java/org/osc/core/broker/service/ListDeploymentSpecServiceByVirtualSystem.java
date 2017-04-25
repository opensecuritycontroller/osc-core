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
package org.osc.core.broker.service;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;

import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.broker.service.api.ListDeploymentSpecServiceByVirtualSystemApi;
import org.osc.core.broker.service.dto.openstack.DeploymentSpecDto;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.DeploymentSpecEntityMgr;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.request.BaseIdRequest;
import org.osc.core.broker.service.response.ListResponse;
import org.osc.core.broker.service.validator.BaseIdRequestValidator;

public class ListDeploymentSpecServiceByVirtualSystem
        extends ServiceDispatcher<BaseIdRequest, ListResponse<DeploymentSpecDto>>
        implements ListDeploymentSpecServiceByVirtualSystemApi {

    private VirtualSystem vs;
    ListResponse<DeploymentSpecDto> response = new ListResponse<DeploymentSpecDto>();

    @Override
    public ListResponse<DeploymentSpecDto> exec(BaseIdRequest request, EntityManager em) throws Exception {

        validateAndLoad(request, em);
        // to do mapping
        List<DeploymentSpecDto> dtoList = new ArrayList<DeploymentSpecDto>();

        // mapping all the da objects to da dto objects
        for (DeploymentSpec ds : DeploymentSpecEntityMgr
                .listDeploymentSpecByVirtualSystem(em, this.vs)) {
            DeploymentSpecDto dto = new DeploymentSpecDto();
            DeploymentSpecEntityMgr.fromEntity(ds, dto);
            dtoList.add(dto);
        }
        this.response.setList(dtoList);
        return this.response;
    }

    private void validateAndLoad(BaseIdRequest req, EntityManager em) throws Exception {
        BaseIdRequestValidator.checkForNullId(req);
        OSCEntityManager<VirtualSystem> emgr = new OSCEntityManager<VirtualSystem>(VirtualSystem.class, em);
        this.vs = emgr.findByPrimaryKey(req.getId());
        if (this.vs == null) {
            throw new VmidcBrokerValidationException("Virtual System with Id: " + req.getId() + "  is not found.");
        }

    }
}
