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
package org.osc.core.broker.service.securityinterface;

import javax.persistence.EntityManager;

import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.service.ServiceDispatcher;
import org.osc.core.broker.service.dto.SecurityGroupInterfaceDto;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.PolicyEntityMgr;
import org.osc.core.broker.service.persistence.VirtualSystemEntityMgr;
import org.osc.core.broker.service.request.Request;
import org.osc.core.broker.service.response.Response;
import org.osc.core.broker.service.validator.SecurityGroupInterfaceDtoValidator;
import org.osgi.service.component.annotations.Reference;

public abstract class BaseSecurityGroupInterfaceService<I extends Request, O extends Response> extends
ServiceDispatcher<I, O> {

    @Reference
    protected ApiFactoryService apiFactoryService;

    protected VirtualSystem validateAndLoad(EntityManager em, SecurityGroupInterfaceDto dto) throws Exception {
        SecurityGroupInterfaceDtoValidator.checkForNullFields(dto);
        SecurityGroupInterfaceDtoValidator.checkFieldLength(dto);

        VirtualSystem vs = VirtualSystemEntityMgr.findById(em, dto.getParentId());

        if (vs == null || vs.getMarkedForDeletion()) {
            throw new VmidcBrokerValidationException("Virtual System with Id: " + dto.getParentId()
            + "  is either not found or is been deleted by the user.");
        }

        if (!this.apiFactoryService.syncsPolicyMapping(vs)) {
            throw new VmidcBrokerValidationException("Security group interfaces cannot be created or updated for appliance manager that does not support policy mapping.");
        }

		// Validate policies
        // TODO Larkins: Improve the method not to do the validation
		PolicyEntityMgr.findPoliciesById(em, dto.getPolicyIds(),
				vs.getDistributedAppliance().getApplianceManagerConnector());

        return vs;
    }

}
