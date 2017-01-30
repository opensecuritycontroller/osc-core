package org.osc.core.broker.model.plugin.manager;

import java.util.ArrayList;
import java.util.List;

import org.osc.sdk.manager.element.SecurityGroupMemberElement;
import org.osc.sdk.manager.element.SecurityGroupMemberListElement;

public class SecurityGroupMemberListElementImpl implements SecurityGroupMemberListElement {

    private List<SecurityGroupMemberElement> members = new ArrayList<>();

    public SecurityGroupMemberListElementImpl(List<SecurityGroupMemberElement> members) {
        this.members = members;
    }

    @Override
    public List<SecurityGroupMemberElement> getMembers() {
        return this.members;
    }

}
