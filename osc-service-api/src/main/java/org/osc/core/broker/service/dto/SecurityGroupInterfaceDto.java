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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.osc.sdk.controller.FailurePolicyType;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * DTO for the security group interface. Note that we assume the DTO reflects only the policy attributes and
 * not the virtual system policy attributes found in the entity.
 */
@ApiModel(description = "Parent Id is applicable for this object. The corresponding Virtual System is considered"
        + " the parent of this Traffic Policy Mapping.<br/>")
@XmlRootElement(name = "securityGroupInterface")
@XmlAccessorType(XmlAccessType.FIELD)
public class SecurityGroupInterfaceDto extends BaseDto {

    @ApiModelProperty(required = true)
    private String name;

    @ApiModelProperty(required = true)
    private String policyName;

    @ApiModelProperty(required = true)
    private Long policyId;

    @ApiModelProperty(required = true)
    private Long tagValue;

    @ApiModelProperty(required = true)
    private boolean userConfigurable = false;

    private String securityGroupName;

    private Long securityGroupId;

    @ApiModelProperty(required = true)
    private FailurePolicyType failurePolicyType;

    @ApiModelProperty(
            value = "Specifies the order of services in which traffic inspection happens. This field is required when binding an Openstack security group with a service."
                    + " This services cannot have the same order or negative order specified")
    private Long order;

    @ApiModelProperty(required = true)
    private boolean markForDeletion = false;

    @Override
    public String toString() {
        return "SecurityGroupInterfaceDto [name=" + this.name + ", policyName=" + this.policyName + ", policyId="
                + this.policyId + ", tagValue=" + this.tagValue + ", userConfigurable=" + this.userConfigurable
                + ", securityGroupName=" + this.securityGroupName + ", securityGroupId=" + this.securityGroupId
                + ", failurePolicyType=" + this.failurePolicyType + "]";
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPolicyName() {
        return this.policyName;
    }

    public void setPolicyName(String virtualSystemPolicyName) {
        this.policyName = virtualSystemPolicyName;
    }

    public Long getPolicyId() {
        return this.policyId;
    }

    public void setPolicyId(Long virtualSystemPolicyId) {
        this.policyId = virtualSystemPolicyId;
    }

    public Long getTagValue() {
        return this.tagValue;
    }

    public void setTagValue(Long tag) {
        this.tagValue = tag;
    }

    public boolean isUserConfigurable() {
        return this.userConfigurable;
    }

    public void setIsUserConfigurable(boolean isUserConfigurable) {
        this.userConfigurable = isUserConfigurable;
    }

    public Long getSecurityGroupId() {
        return this.securityGroupId;
    }

    public void setSecurityGroupId(Long securityGroupId) {
        this.securityGroupId = securityGroupId;
    }

    public String getSecurityGroupName() {
        return this.securityGroupName;
    }

    public void setSecurityGroupName(String securityGroupName) {
        this.securityGroupName = securityGroupName;
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

    public boolean isMarkForDeletion() {
        return this.markForDeletion;
    }

    public void setMarkForDeletion(boolean markForDeletion) {
        this.markForDeletion = markForDeletion;
    }

}
