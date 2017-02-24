package org.osc.core.broker.service.securitygroup;

import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.osc.core.broker.service.request.BaseIdRequest;

@XmlRootElement(name = "updateMemberRequest")
@XmlAccessorType(XmlAccessType.FIELD)
public class UpdateSecurityGroupMemberRequest extends BaseIdRequest {

    private Set<SecurityGroupMemberItemDto> members = new HashSet<>();

    public Set<SecurityGroupMemberItemDto> getMembers() {
        return this.members;
    }

    public void setMembers(Set<SecurityGroupMemberItemDto> members) {
        this.members = members;
    }

}
