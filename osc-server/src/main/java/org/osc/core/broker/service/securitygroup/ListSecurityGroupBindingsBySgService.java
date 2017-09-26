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
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.management.Policy;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupInterface;
import org.osc.core.broker.model.entities.virtualization.ServiceFunctionChain;
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.service.ServiceDispatcher;
import org.osc.core.broker.service.api.ListSecurityGroupBindingsBySgServiceApi;
import org.osc.core.broker.service.dto.PolicyDto;
import org.osc.core.broker.service.dto.VirtualSystemPolicyBindingDto;
import org.osc.core.broker.service.dto.VirtualizationConnectorDto;
import org.osc.core.broker.service.exceptions.ActionNotSupportedException;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.PolicyEntityMgr;
import org.osc.core.broker.service.persistence.SecurityGroupEntityMgr;
import org.osc.core.broker.service.request.BaseIdRequest;
import org.osc.core.broker.service.response.BindSecurityGroupResponse;
import org.osc.core.broker.service.validator.BaseIdRequestValidator;
import org.osc.core.common.virtualization.VirtualizationType;
import org.osc.sdk.controller.FailurePolicyType;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component
public class ListSecurityGroupBindingsBySgService
        extends ServiceDispatcher<BaseIdRequest, BindSecurityGroupResponse>
        implements ListSecurityGroupBindingsBySgServiceApi {

    @Reference
    private ApiFactoryService apiFactoryService;

    @Override
    public BindSecurityGroupResponse exec(BaseIdRequest request, EntityManager em) throws Exception {
    	
    	BindSecurityGroupResponse response = new BindSecurityGroupResponse();

        SecurityGroup sg = validate(em, request);
        
        // to do mapping
        List<VirtualSystemPolicyBindingDto> dtoList = new ArrayList<>();
        Set<VirtualSystem> vsSet = sg.getVirtualizationConnector().getVirtualSystems();
        long order = -1;

        // Existing bindings
        for (SecurityGroupInterface sgInterface : sg.getSecurityGroupInterfaces()) {
            VirtualSystem vs = sgInterface.getVirtualSystem();
            vsSet.remove(vs);
			Set<Long> policyIds = sgInterface.getMgrPolicies().stream().map(Policy::getId).collect(Collectors.toSet());
            VirtualSystemPolicyBindingDto virtualSystemBindingDto = new VirtualSystemPolicyBindingDto(vs.getId(),
                    vs.getDistributedAppliance().getName(), policyIds,
                    FailurePolicyType.valueOf(sgInterface.getFailurePolicyType().name()), sgInterface.getOrder());
            virtualSystemBindingDto.setMarkedForDeletion(sgInterface.getMarkedForDeletion());
            virtualSystemBindingDto.setBinded(true);
			virtualSystemBindingDto.setMultiplePoliciesSupported(this.apiFactoryService.supportsMultiplePolicies(vs));
            if (vs.getDomain() != null) {
                for (Policy policy : vs.getDomain().getPolicies()) {
                    PolicyDto dto = new PolicyDto();
                    PolicyEntityMgr.fromEntity(policy, dto);
                    virtualSystemBindingDto.addPolicies(dto);
                }
            }

            dtoList.add(virtualSystemBindingDto);
            order++;
        }

        // Other available Bindings
        if (sg.getVirtualizationConnector().getVirtualizationType() == VirtualizationType.OPENSTACK) {
            FailurePolicyType failurePolicyType = this.apiFactoryService.supportsFailurePolicy(sg)
                    ? FailurePolicyType.FAIL_OPEN : FailurePolicyType.NA;

            for (VirtualSystem vs : vsSet) {
                // Only allow binding to non-deleted services
                if (!vs.getMarkedForDeletion()) {
                    order++;
                    // Checking if the SDN controller supports failure policy.
                    // If yes giving the default Failure Policy Type value
                    // FAIL_OPEN
                    VirtualSystemPolicyBindingDto virtualSystemBindingDto = new VirtualSystemPolicyBindingDto(
                            vs.getId(), vs.getDistributedAppliance().getName(), null, failurePolicyType, order);
					virtualSystemBindingDto
							.setMultiplePoliciesSupported(this.apiFactoryService.supportsMultiplePolicies(vs));

                    if (vs.getDomain() != null) {
                        for (Policy policy : vs.getDomain().getPolicies()) {
                            PolicyDto dto = new PolicyDto();
                            PolicyEntityMgr.fromEntity(policy, dto);
                            virtualSystemBindingDto.addPolicies(dto);
                        }
                    }

                    dtoList.add(virtualSystemBindingDto);
                }
            }

            if (dtoList.size() == 0) {
                throw new ActionNotSupportedException(
                        "Invalid Action. There are no Distributed Appliances configured with this virtualization connector.");
            }
        }

		ServiceFunctionChain sfc  = sg.getServiceFunctionChain();
        if(sfc != null) {
        	response.setSfcId(sfc.getId());
        }
        response.setMemberList(dtoList);
        return response;
    }

    protected SecurityGroup validate(EntityManager em, BaseIdRequest request) throws Exception {
        BaseIdRequestValidator.checkForNullId(request);

        SecurityGroup sg = SecurityGroupEntityMgr.findById(em, request.getId());

        if (sg == null) {
            throw new VmidcBrokerValidationException("Security Group with Id: " + request.getId() + "  is not found.");
        }

        if (sg.getVirtualizationConnector().getControllerType().equals(VirtualizationConnectorDto.CONTROLLER_TYPE_NONE)
                && sg.getVirtualizationConnector().getVirtualizationType() == VirtualizationType.OPENSTACK) {
            throw new ActionNotSupportedException(
                    "Invalid Action. Controller is not defined for this Virtualization Connector.");
        }
        return sg;
    }
}
