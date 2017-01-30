package org.osc.core.broker.service.policy;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.osc.core.broker.service.dto.BaseDto;

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
