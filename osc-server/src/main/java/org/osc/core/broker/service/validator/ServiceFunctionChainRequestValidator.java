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
package org.osc.core.broker.service.validator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import org.apache.commons.collections4.CollectionUtils;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.ServiceFunctionChain;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.service.dto.BaseDto;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.persistence.SecurityGroupEntityMgr;
import org.osc.core.broker.service.persistence.VirtualSystemEntityMgr;
import org.osc.core.broker.service.persistence.VirtualizationConnectorEntityMgr;
import org.osc.core.broker.service.request.AddOrUpdateServiceFunctionChainRequest;
import org.osc.core.broker.util.TransactionalBroadcastUtil;
import org.osc.core.broker.util.ValidateUtil;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = ServiceFunctionChainRequestValidator.class)
public class ServiceFunctionChainRequestValidator
		implements RequestValidator<AddOrUpdateServiceFunctionChainRequest, ServiceFunctionChain> {

    private EntityManager em;

    @Reference
	private TransactionalBroadcastUtil txBroadcastUtil;

	@Reference
	public ApiFactoryService apiFactoryService;

	public ServiceFunctionChainRequestValidator create(EntityManager em) {
		ServiceFunctionChainRequestValidator validator = new ServiceFunctionChainRequestValidator();
		validator.em = em;
		validator.apiFactoryService = this.apiFactoryService;
		validator.txBroadcastUtil = this.txBroadcastUtil;
		return validator;
	}

	@Override
	public void validate(AddOrUpdateServiceFunctionChainRequest request) throws Exception {
		checkForNullFields(request, true);

		if (!ValidateUtil.validateDaName(request.getName())) {
			throw new VmidcBrokerValidationException("Invalid Service Function Name: " + request.getName()
					+ "SFC name must not exceed 13 characters, must start with a letter, and can only contain numbers, letters and dash(-).");
		}
		validateCommon(request);

		OSCEntityManager<ServiceFunctionChain> emgr = new OSCEntityManager<ServiceFunctionChain>(
				ServiceFunctionChain.class, this.em, this.txBroadcastUtil);

		if (emgr.isExisting("name", request.getName())) {
			throw new VmidcBrokerValidationException("Service Function Name: " + request.getName() + " already exists.");
		}
	}

	@Override
	public ServiceFunctionChain validateAndLoad(AddOrUpdateServiceFunctionChainRequest request) throws Exception {
		// check for sfc and vc id for update
	    checkForNullFields(request, false);

		ServiceFunctionChain sfc = this.em.find(ServiceFunctionChain.class, request.getDto().getId());

		if (sfc == null) {
			throw new VmidcBrokerValidationException(
					"Service Function Chain entry with name: " + request.getName() + " is not found.");
		}
		ValidateUtil.checkMarkedForDeletion(sfc, sfc.getName());
		validateCommon(request);

        //Throw an exception if this sfc is already binded to a security group.
        List<SecurityGroup> sgList = SecurityGroupEntityMgr.listSecurityGroupsBySfcId(this.em, sfc.getId());
        String sgNames = sgList.stream().filter(sg -> !sg.getMarkedForDeletion()).map(sg -> sg.getName()).collect(Collectors.joining(", "));
        if (!sgNames.isEmpty()) {
            throw new VmidcBrokerValidationException(
                    String.format("Cannot update Service Function Chain: '%s', as it is binded to Security Group(s) '%s'",
                            sfc.getName(), sgNames));
        }

		return sfc;
	}

	void validateCommon(AddOrUpdateServiceFunctionChainRequest request) throws Exception {

		BaseDto dto = request.getDto();

		VirtualizationConnector vc = validateVirtualConnector(this.em, dto.getParentId());

		validateVirtualSystems(request.getVirtualSystemIds(), vc);
	}

    private void checkForNullFields(AddOrUpdateServiceFunctionChainRequest request, boolean forCreate)
            throws Exception {

        // build a map of (field,value) pairs to be checked for null/empty
        // values
        Map<String, Object> map = new HashMap<String, Object>();

        map.put("Service Function Chain Name", request.getName());
        map.put("Dto", request.getDto());
        if (request.getDto() != null) {
            // if dto is null, previous check catches it
            if (!forCreate) {
                map.put("Id", request.getDto().getId());
            }
            map.put("Virtualization Connector Id", request.getDto().getParentId());
        }
        ValidateUtil.checkForNullFields(map);
    }

	public VirtualizationConnector validateVirtualConnector(EntityManager em, Long vcId) throws Exception {

		VirtualizationConnector vc = VirtualizationConnectorEntityMgr.findById(em, vcId);

		if (vc == null) {
			throw new VmidcBrokerValidationException("Virtualization Connector id " + vcId + " is not found.");
		}

		if (!vc.isControllerDefined() || !this.apiFactoryService.supportsNeutronSFC(vc.getControllerType())) {
			throw new VmidcBrokerValidationException(
					"Creation of Service Function Chain is not allowed with controller of type " + vc.getControllerType());
		}

		return vc;
	}

	public void validateVirtualSystems(List<Long> vsIds, VirtualizationConnector vc) throws Exception {
		// make sure all the virtual system in the sfc dto list exist otherwise
		// throw
        for (Long vsId : CollectionUtils.emptyIfNull(vsIds)) {
            VirtualSystem virtualSystem = VirtualSystemEntityMgr.findById(this.em, vsId);
            if (virtualSystem == null) {
                throw new VmidcBrokerValidationException("VirtualSytem with id " + vsId + " is not found.");
            } else if (!vc.getVirtualSystems().contains(virtualSystem)) {
                throw new VmidcBrokerValidationException(String.format(
                        "Virtual system with id %s and with Virtualization Connector id %s does not match the virtualization connector"
                                + " under which this Service function chain is being created/updated",
                        vsId, virtualSystem.getVirtualizationConnector().getId()));
            }
        }

	}

}
