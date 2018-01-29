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

import org.apache.commons.collections4.CollectionUtils;
import org.osc.core.broker.model.entities.virtualization.ServiceFunctionChain;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.service.ServiceDispatcher;
import org.osc.core.broker.service.api.AddServiceFunctionChainServiceApi;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.persistence.VirtualSystemEntityMgr;
import org.osc.core.broker.service.persistence.VirtualizationConnectorEntityMgr;
import org.osc.core.broker.service.request.AddOrUpdateServiceFunctionChainRequest;
import org.osc.core.broker.service.response.BaseResponse;
import org.osc.core.broker.service.validator.RequestValidator;
import org.osc.core.broker.service.validator.ServiceFunctionChainRequestValidator;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component
public class AddServiceFunctionChainService
extends ServiceDispatcher<AddOrUpdateServiceFunctionChainRequest, BaseResponse>
implements AddServiceFunctionChainServiceApi {

    RequestValidator<AddOrUpdateServiceFunctionChainRequest, ServiceFunctionChain> validator;

    @Reference
    private ServiceFunctionChainRequestValidator validatorFactory;

    @Override
    public BaseResponse exec(AddOrUpdateServiceFunctionChainRequest request, EntityManager em) throws Exception {

        if (this.validator == null) {
            this.validator = this.validatorFactory.create(em);
        }
        this.validator.validate(request);

        VirtualizationConnector vc = VirtualizationConnectorEntityMgr.findById(em, request.getDto().getParentId());

        ServiceFunctionChain sfc = new ServiceFunctionChain(request.getName(), vc);

        for (Long vsId : CollectionUtils.emptyIfNull(request.getVirtualSystemIds())) {
            sfc.addVirtualSystem(VirtualSystemEntityMgr.findById(em, vsId));
        }

        OSCEntityManager.create(em, sfc, this.txBroadcastUtil);

        BaseResponse response = new BaseResponse();
        response.setId(sfc.getId());

        return response;

    }
}
