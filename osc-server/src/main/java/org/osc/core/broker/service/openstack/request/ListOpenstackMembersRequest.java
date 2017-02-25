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
