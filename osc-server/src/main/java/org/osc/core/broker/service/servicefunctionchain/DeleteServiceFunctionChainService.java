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
import org.osc.core.broker.service.ServiceDispatcher;
import org.osc.core.broker.service.api.DeleteServiceFunctionChainServiceApi;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.request.BaseIdRequest;
import org.osc.core.broker.service.response.EmptySuccessResponse;
import org.osc.core.broker.service.validator.BaseIdRequestValidator;
import org.osc.core.broker.service.validator.ServiceFunctionChainRequestValidator;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component
public class DeleteServiceFunctionChainService extends ServiceDispatcher<BaseIdRequest, EmptySuccessResponse>
implements DeleteServiceFunctionChainServiceApi {

    @Reference
    protected ServiceFunctionChainRequestValidator validator;

    @Override
    public EmptySuccessResponse exec(BaseIdRequest request, EntityManager em) throws Exception {

        BaseIdRequestValidator.checkForNullIdAndParentNullId(request);

        this.validator.create(em).validateVirtualConnector(em, request.getParentId());

        ServiceFunctionChain sfc = em.find(ServiceFunctionChain.class, request.getId());
        if (sfc == null) {
            throw new VmidcBrokerValidationException("Service Function Chain with Id " + request.getId() + " is not found.");
        }

        // Initializing Entity Manager
        OSCEntityManager<ServiceFunctionChain> emgr = new OSCEntityManager<ServiceFunctionChain>(ServiceFunctionChain.class, em, this.txBroadcastUtil);

        emgr.delete(request.getId());

        EmptySuccessResponse response = new EmptySuccessResponse();
        return response;
    }

}
