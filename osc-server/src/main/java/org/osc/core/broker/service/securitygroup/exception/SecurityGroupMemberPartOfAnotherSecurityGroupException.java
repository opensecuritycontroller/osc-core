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
package org.osc.core.broker.service.securitygroup.exception;

import org.osc.core.broker.service.common.VmidcMessages;
import org.osc.core.broker.service.common.VmidcMessages_;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;

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
