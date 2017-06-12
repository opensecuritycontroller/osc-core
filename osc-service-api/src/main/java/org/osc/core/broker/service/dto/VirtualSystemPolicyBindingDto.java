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
import java.util.Comparator;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.osc.sdk.controller.FailurePolicyType;

import io.swagger.annotations.ApiModelProperty;

@XmlRootElement(name = "virtualSystemPolicyBinding")
@XmlAccessorType(XmlAccessType.FIELD)
public class VirtualSystemPolicyBindingDto {

    /**
     * Compares the VirtualSystemPolicyBindingDto using the order.
     * "Note: this comparator imposes orderings that are inconsistent with equals."
     */
    public static class VirtualSystemPolicyBindingDtoComparator implements Comparator<VirtualSystemPolicyBindingDto> {

        @Override
        public int compare(VirtualSystemPolicyBindingDto o1, VirtualSystemPolicyBindingDto o2) {
            return Long.compare(o1.getOrder(), o2.getOrder());
        }
    }

    @ApiModelProperty(required = true)
    private Long virtualSystemId;

    @ApiModelProperty(required = true)
    private String name;

    @ApiModelProperty(required = true)
    private Long policyId;

    @ApiModelProperty(
            value = "Failure policy required only if the SDN controller supports Failure policy else defaults to NA",
            required = true)
    private FailurePolicyType failurePolicyType;

    @ApiModelProperty(
            value = "The order in which the security services will be applied. Negative/duplicate orders for bindings will result in an error.",
            required = true)
    private Long order;

    /**
     * List of Policies owned by the Virtual System
     */
    private List<PolicyDto> policies = new ArrayList<>();

    @ApiModelProperty(required = true)
    private boolean isBinded;

    @ApiModelProperty(readOnly = true)
    private boolean markedForDeletion;

    public VirtualSystemPolicyBindingDto() {
    }

    public VirtualSystemPolicyBindingDto(Long virtualSystemId, String name, Long policyId,
            FailurePolicyType failurePolicyType, long order) {
        super();
        this.virtualSystemId = virtualSystemId;
        this.name = name;
        this.policyId = policyId;
        this.failurePolicyType = failurePolicyType;
        this.order = order;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getVirtualSystemId() {
        return this.virtualSystemId;
    }

    public Long getPolicyId() {
        return this.policyId;
    }

    public FailurePolicyType getFailurePolicyType() {
        return this.failurePolicyType;
    }

    public void setFailurePolicyType(FailurePolicyType failurePolicyType) {
        this.failurePolicyType = failurePolicyType;
    }

    public Long getOrder() {
        return this.order;
    }

    public void setOrder(long order) {
        this.order = order;
    }

    public List<PolicyDto> getPolicies() {
        return this.policies;
    }

    public void addPolicies(PolicyDto policy) {
        this.policies.add(policy);
    }

    public boolean isBinded() {
        return this.isBinded;
    }

    public void setBinded(boolean selected) {
        this.isBinded = selected;
    }

    public boolean isMarkedForDeletion() {
        return this.markedForDeletion;
    }

    public void setMarkedForDeletion(boolean markedForDeletion) {
        this.markedForDeletion = markedForDeletion;
    }

    @Override
    public String toString() {
        return "VirtualSystemPolicyBindingDto [virtualSystemId=" + this.virtualSystemId + ", name=" + this.name + ", policyId="
                + this.policyId + ", failurePolicyType=" + this.failurePolicyType + ", order=" + this.order
                + ", policies=" + this.policies + ", isBinded=" + this.isBinded + ", markedForDeletion=" + this.markedForDeletion
                + "]";
    }

}
