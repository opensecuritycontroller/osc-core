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
package org.osc.core.broker.service.persistence;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMember;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMemberType;
import org.osc.core.broker.model.entities.virtualization.openstack.Network;
import org.osc.core.broker.model.entities.virtualization.openstack.Subnet;
import org.osc.core.broker.model.entities.virtualization.openstack.VM;
import org.osc.core.broker.service.securitygroup.SecurityGroupMemberItemDto;

public class SecurityGroupMemberEntityMgr {

    public static void fromEntity(SecurityGroupMember entity, SecurityGroupMemberItemDto dto) {
        SecurityGroupMemberType type = entity.getType();

        dto.setId(entity.getId());
        dto.setType(type);
        if (type == SecurityGroupMemberType.VM) {

            VM vm = entity.getVm();
            dto.setName(vm.getName());
            dto.setOpenstackId(vm.getOpenstackId());
            dto.setRegion(vm.getRegion());
        } else if (type == SecurityGroupMemberType.NETWORK) {

            Network nw = entity.getNetwork();
            dto.setName(nw.getName());
            dto.setOpenstackId(nw.getOpenstackId());
            dto.setRegion(nw.getRegion());

        } else if (type == SecurityGroupMemberType.SUBNET) {

            Subnet subnet = entity.getSubnet();
            dto.setName(subnet.getName());
            dto.setOpenstackId(subnet.getOpenstackId());
            dto.setRegion(subnet.getRegion());
            dto.setParentOpenStackId(subnet.getNetworkId());
            dto.setProtectExternal(subnet.isProtectExternal());

        }

    }

    @SuppressWarnings("unchecked")
    public static List<SecurityGroupMember> listActiveSecurityGroupMembersBySecurityGroup(Session session,
            SecurityGroup sg) {
        Criteria criteria = session.createCriteria(SecurityGroupMember.class)
                .add(Restrictions.eq("markedForDeletion", false)).add(Restrictions.eq("securityGroup", sg))
                .addOrder(Order.asc("type")).setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
        return criteria.list();
    }

}
