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

import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.management.Policy;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupInterface;
import org.osc.core.broker.service.securityinterface.SecurityGroupInterfaceDto;
import org.osc.sdk.controller.FailurePolicyType;

public class SecurityGroupInterfaceEntityMgr {

    public static void toEntity(SecurityGroupInterface sgi, SecurityGroupInterfaceDto dto, Policy policy,
            String tagPrefix) {
        sgi.setName(dto.getName());
        sgi.setTag(dto.getTagValue() == null ? null : tagPrefix + dto.getTagValue().toString());
        sgi.setPolicy(policy);
    }

    public static void toEntity(SecurityGroupInterface sgi, SecurityGroup sg, String serviceName) {
        sgi.setName(serviceName + "_" + sg.getId());
        sgi.setUserConfigurable(false);
    }

    public static void fromEntity(SecurityGroupInterface sgi, SecurityGroupInterfaceDto dto) {
        dto.setId(sgi.getId());
        dto.setParentId(sgi.getVirtualSystem().getId());
        dto.setName(sgi.getName());
        dto.setTagValue(sgi.getTagValue());
        dto.setPolicyId(sgi.getMgrPolicy() != null ? sgi.getMgrPolicy().getId() : null);
        dto.setPolicyName(sgi.getMgrPolicy() != null ? sgi.getMgrPolicy().getName() : null);
        dto.setIsUserConfigurable(sgi.isUserConfigurable());
        dto.setSecurityGroupId(sgi.getSecurityGroup() != null ? sgi.getSecurityGroup().getId() : null);
        dto.setSecurityGroupName(sgi.getSecurityGroup() != null ? sgi.getSecurityGroup().getName() : null);
        dto.setFailurePolicyType(FailurePolicyType.valueOf(
                sgi.getFailurePolicyType().name()));
        dto.setMarkForDeletion(sgi.getMarkedForDeletion());
        dto.setOrder(sgi.getOrder());
    }

    public static SecurityGroupInterface findSecurityGroupInterfacesByVsAndSecurityGroup(EntityManager em,
            VirtualSystem vs, SecurityGroup sg) {

        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<SecurityGroupInterface> query = cb.createQuery(SecurityGroupInterface.class);

        Root<SecurityGroupInterface> root = query.from(SecurityGroupInterface.class);

        query = query.select(root)
                .where(cb.equal(root.join("securityGroups").get("id"), sg.getId()),
                       cb.equal(root.get("virtualSystem"), vs));

        return em.createQuery(query).getSingleResult();
    }

    public static SecurityGroupInterface findSecurityGroupInterfaceByVsAndTag(EntityManager em, VirtualSystem vs,
            String tag) {
        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<SecurityGroupInterface> query = cb.createQuery(SecurityGroupInterface.class);

        Root<SecurityGroupInterface> root = query.from(SecurityGroupInterface.class);

        query = query.select(root)
                .where(cb.equal(root.get("tag"), tag),
                       cb.equal(root.get("virtualSystem"), vs));

        List<SecurityGroupInterface> list = em.createQuery(query).setMaxResults(1).getResultList();

        if (list == null || list.size() == 0) {
            return null;
        }

        return list.get(0);
    }

    public static SecurityGroupInterface findById(EntityManager em, Long id) {
        OSCEntityManager<SecurityGroupInterface> emgr = new OSCEntityManager<SecurityGroupInterface>(
                SecurityGroupInterface.class, em);
        return emgr.findByPrimaryKey(id);
    }

}
