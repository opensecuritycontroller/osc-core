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

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMember;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMemberType;
import org.osc.core.broker.model.entities.virtualization.openstack.Network;
import org.osc.core.broker.model.entities.virtualization.openstack.Subnet;
import org.osc.core.broker.model.entities.virtualization.openstack.VM;
import org.osc.core.broker.service.dto.SecurityGroupMemberItemDto;

public class SecurityGroupMemberEntityMgr {

    public static void fromEntity(SecurityGroupMember entity, SecurityGroupMemberItemDto dto) {
        SecurityGroupMemberType type = entity.getType();

        dto.setId(entity.getId());
        dto.setType(type.toString());
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

    public static List<SecurityGroupMember> listActiveSecurityGroupMembersBySecurityGroup(EntityManager em,
            SecurityGroup sg) {
        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<SecurityGroupMember> query = cb.createQuery(SecurityGroupMember.class);

        Root<SecurityGroupMember> root = query.from(SecurityGroupMember.class);

        query = query.select(root).distinct(true)
            .where(cb.equal(root.get("markedForDeletion"), false),
                   cb.equal(root.get("securityGroup"), sg))
            .orderBy(cb.asc(root.get("type")));


        return em.createQuery(query).getResultList();
    }

}
