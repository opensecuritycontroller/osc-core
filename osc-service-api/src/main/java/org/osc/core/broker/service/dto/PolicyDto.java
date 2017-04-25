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

import io.swagger.annotations.ApiModelProperty;

@XmlRootElement(name = "policy")
@XmlAccessorType(XmlAccessType.FIELD)
public class PolicyDto extends BaseDto {

    @ApiModelProperty(value = "The Policy name", required = true, readOnly = true)
    private String policyName;

    @ApiModelProperty(value = "The Policy Id on the manager", required = true, readOnly = true)
    private String mgrPolicyId;

    @ApiModelProperty(value = "The Domain Id on the manager", required = true, readOnly = true)
    private Long mgrDomainId;

    @ApiModelProperty(value = "The Domain name", required = true, readOnly = true)
    private String mgrDomainName;

    public String getPolicyName() {
        return this.policyName;
    }

    public void setPolicyName(String name) {
        this.policyName = name;
    }

    public String getMgrPolicyId() {
        return this.mgrPolicyId;
    }

    public void setMgrPolicyId(String mgrPolicyId) {
        this.mgrPolicyId = mgrPolicyId;
    }

    public void setMgrDomainId(Long domainId) {
        this.mgrDomainId = domainId;
    }

    public Long getMgrDomainId() {
        return this.mgrDomainId;
    }

    public String getMgrDomainName() {
        return this.mgrDomainName;
    }

    public void setMgrDomainName(String mgrDomainName) {
        this.mgrDomainName = mgrDomainName;
    }

}
