package org.osc.core.broker.service.securitygroup;

import java.util.HashSet;
import java.util.Set;

import org.osc.core.broker.service.request.BaseRequest;

public class AddOrUpdateSecurityGroupRequest extends BaseRequest<SecurityGroupDto> {

    private Set<SecurityGroupMemberItemDto> members = new HashSet<>();

    public Set<SecurityGroupMemberItemDto> getMembers() {
        return this.members;
    }

    public void setMembers(Set<SecurityGroupMemberItemDto> members) {
        this.members = members;
    }

}
