package org.osc.core.broker.service.securitygroup.exception;

import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.view.common.VmidcMessages;
import org.osc.core.broker.view.common.VmidcMessages_;

@SuppressWarnings("serial")
public class SecurityGroupMemberPartOfAnotherSecurityGroupException extends VmidcBrokerValidationException {

    private String memberName;

    public SecurityGroupMemberPartOfAnotherSecurityGroupException(String memberName, String exceptionMessage) {
        super(exceptionMessage);
        this.memberName = memberName;
    }

    public static String getDefaultExceptionMessage(String memberName, String otherSecurityGroup) {
        return VmidcMessages.getString(VmidcMessages_.SG_OVERLAP_DEFAULT, memberName, otherSecurityGroup);
    }

    public String getMemberName() {
        return this.memberName;
    }

    public void setMemberName(String memberName) {
        this.memberName = memberName;
    }
}
