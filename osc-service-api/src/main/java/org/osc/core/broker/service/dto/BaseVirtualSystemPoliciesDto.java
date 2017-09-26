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
package org.osc.core.broker.service.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import io.swagger.annotations.ApiModelProperty;

@XmlRootElement(name = "BaseVirtualSystemPolicies")
@XmlAccessorType(XmlAccessType.FIELD)
public class BaseVirtualSystemPoliciesDto {
	
	@ApiModelProperty(required = true)
    private Long virtualSystemId;

    @ApiModelProperty(required = true)
    private String name;

    @ApiModelProperty(required = true)
    private Set<Long> policyIds;
    
    /**
     * List of Policies owned by the Virtual System
     */
    private List<PolicyDto> policies = new ArrayList<>();
    
    @ApiModelProperty(readOnly = true,
			value = "Determines whether the appliance manager supports multiple policy mapping.")
    @XmlElement(name = "multiplePoliciesSupported")
	private Boolean isMultiplePoliciesSupported;

    @ApiModelProperty(readOnly = true)
    private boolean markedForDeletion;

    
	public BaseVirtualSystemPoliciesDto() {
		super();
	}
	
	public BaseVirtualSystemPoliciesDto(Long virtualSystemId, String name, Set<Long> policyIds, List<PolicyDto> policies) {
		super();
		this.virtualSystemId = virtualSystemId;
		this.name = name;
		this.policyIds = policyIds;
		this.policies = policies;
	}

	public BaseVirtualSystemPoliciesDto(Long virtualSystemId, String name, Set<Long> policyIds) {
		super();
		this.virtualSystemId = virtualSystemId;
		this.name = name;
		this.policyIds = policyIds;
	}

	public Long getVirtualSystemId() {
		return virtualSystemId;
	}

	public void setVirtualSystemId(Long virtualSystemId) {
		this.virtualSystemId = virtualSystemId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Set<Long> getPolicyIds() {
		return policyIds;
	}

	public void setPolicyIds(Set<Long> policyIds) {
		this.policyIds = policyIds;
	}

	public List<PolicyDto> getPolicies() {
		return policies;
	}

	public void setPolicies(List<PolicyDto> policies) {
		this.policies = policies;
	}

	public Boolean isMultiplePoliciesSupported() {
		return isMultiplePoliciesSupported;
	}

	public void setMultiplePoliciesSupported(Boolean isMultiplePoliciesSupported) {
		this.isMultiplePoliciesSupported = isMultiplePoliciesSupported;
	}

	public boolean isMarkedForDeletion() {
		return markedForDeletion;
	}

	public void setMarkedForDeletion(boolean markedForDeletion) {
		this.markedForDeletion = markedForDeletion;
	}
	
	public void addPolicies(PolicyDto policy) {
        this.policies.add(policy);
    }
    
}
