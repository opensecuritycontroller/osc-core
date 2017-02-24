package org.osc.core.broker.service.openstack.request;

import java.util.Set;

import org.osc.core.broker.model.entities.virtualization.SecurityGroupMemberType;
import org.osc.core.broker.service.securitygroup.SecurityGroupMemberItemDto;

public class ListOpenstackMembersRequest extends BaseOpenStackRequest {

    private SecurityGroupMemberType type;
    private Set<SecurityGroupMemberItemDto> currentSelectedMembers;

    public SecurityGroupMemberType getType() {
        return this.type;
    }

    public void setType(SecurityGroupMemberType type) {
        this.type = type;
    }

    public Set<SecurityGroupMemberItemDto> getCurrentSelectedMembers() {
        return this.currentSelectedMembers;
    }

    public void setCurrentSelectedMembers(Set<SecurityGroupMemberItemDto> currentSelectedMembers) {
        this.currentSelectedMembers = currentSelectedMembers;
    }

}
