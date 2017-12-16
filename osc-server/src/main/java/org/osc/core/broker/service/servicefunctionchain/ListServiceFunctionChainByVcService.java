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
package org.osc.core.broker.service.servicefunctionchain;

import javax.persistence.EntityManager;

import org.osc.core.broker.model.entities.virtualization.ServiceFunctionChain;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.service.ServiceDispatcher;
import org.osc.core.broker.service.api.ListServiceFunctionChainByVcServiceApi;
import org.osc.core.broker.service.dto.ServiceFunctionChainDto;
import org.osc.core.broker.service.persistence.ServiceFunctionChainEntityMgr;
import org.osc.core.broker.service.request.BaseIdRequest;
import org.osc.core.broker.service.response.ListResponse;
import org.osc.core.broker.service.validator.ServiceFunctionChainRequestValidator;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component
public class ListServiceFunctionChainByVcService
        extends ServiceDispatcher<BaseIdRequest, ListResponse<ServiceFunctionChainDto>>
        implements ListServiceFunctionChainByVcServiceApi {

    @Reference
    protected ServiceFunctionChainRequestValidator validator;

    @Override
    public ListResponse<ServiceFunctionChainDto> exec(BaseIdRequest request, EntityManager em) throws Exception {
        ListResponse<ServiceFunctionChainDto> response = new ListResponse<ServiceFunctionChainDto>();
        
        VirtualizationConnector vc = this.validator.create(em).validateVirtualConnector(em, request.getId());
     

        for (ServiceFunctionChain sfc : vc.getServiceFunctionChains()) {
            ServiceFunctionChainDto sfcDto = new ServiceFunctionChainDto();
            ServiceFunctionChainEntityMgr.fromEntity(sfc, sfcDto);
            response.getList().add(sfcDto);
        }

        return response;
    }
}
