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

import java.util.Comparator;
import java.util.List;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.osc.sdk.controller.FailurePolicyType;

import io.swagger.annotations.ApiModelProperty;

@XmlRootElement(name = "virtualSystemPolicyBinding")
@XmlAccessorType(XmlAccessType.FIELD)
public class VirtualSystemPolicyBindingDto extends BaseVirtualSystemPoliciesDto {

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

    @ApiModelProperty(
            value = "Failure policy required only if the SDN controller supports Failure policy else defaults to NA",
            required = true)
    private FailurePolicyType failurePolicyType;

    @ApiModelProperty(
            value = "The order in which the security services will be applied. Negative/duplicate orders for bindings will result in an error.",
            required = true)
    private Long order;

    @ApiModelProperty(required = true)
    private boolean binded;

    public VirtualSystemPolicyBindingDto() {

    }

    public VirtualSystemPolicyBindingDto(Long virtualSystemId, String name, Set<Long> policyIds, List<PolicyDto> policies) {
		super(virtualSystemId, name, policyIds, policies);
    }

	public VirtualSystemPolicyBindingDto(Long virtualSystemId, String name, Set<Long> policyIds,
            FailurePolicyType failurePolicyType, Long order) {
		super(virtualSystemId, name, policyIds);
        this.failurePolicyType = failurePolicyType;
        this.order = order;
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

    public boolean isBinded() {
        return this.binded;
    }

    public void setBinded(boolean selected) {
        this.binded = selected;
    }

    @Override
    public String toString() {
        return "VirtualSystemPolicyBindingDto [virtualSystemId=" + getVirtualSystemId() + ", name=" + getName() + ", policyId="
                + getPolicyIds() + ", failurePolicyType=" + this.failurePolicyType + ", order=" + this.order
                + ", policies=" + getPolicies() + ", binded=" + this.binded + ", markedForDeletion=" + isMarkedForDeletion()
                + "]";
    }

}
