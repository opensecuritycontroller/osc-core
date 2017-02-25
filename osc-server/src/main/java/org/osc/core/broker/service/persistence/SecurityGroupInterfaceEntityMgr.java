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
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.management.Policy;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupInterface;
import org.osc.core.broker.service.securityinterface.SecurityGroupInterfaceDto;

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
        dto.setFailurePolicyType(sgi.getFailurePolicyType());
        dto.setMarkForDeletion(sgi.getMarkedForDeletion());
        dto.setOrder(sgi.getOrder());
    }

    public static SecurityGroupInterface findSecurityGroupInterfacesByVsAndSecurityGroup(Session session,
            VirtualSystem vs, SecurityGroup sg) {
        Criteria criteria = session.createCriteria(SecurityGroupInterface.class, "sgi")
                .createAlias("sgi.securityGroups", "sg").add(Restrictions.eq("sg.id", sg.getId()))
                .add(Restrictions.eq("sgi.virtualSystem", vs));

        return (SecurityGroupInterface) criteria.uniqueResult();
    }

    public static SecurityGroupInterface findSecurityGroupInterfaceByVsAndTag(Session session, VirtualSystem vs,
            String tag) {

        Criteria criteria = session.createCriteria(SecurityGroupInterface.class)
                .add(Restrictions.eq("virtualSystem", vs)).add(Restrictions.eq("tag", tag));

        @SuppressWarnings("unchecked")
        List<SecurityGroupInterface> list = criteria.setFirstResult(0).setMaxResults(1).list();

        if (list == null || list.size() == 0) {
            return null;
        }

        return list.get(0);
    }

    public static List<SecurityGroupInterface> listAllConfigurableSecurityGroupInterfaces(Session session,
            Order[] orders) {
        return listAllSecurityGroupInterfaces(session, true, orders);
    }

    public static List<SecurityGroupInterface> listAllNonConfigurableSecurityGroupInterfaces(Session session,
            Order[] orders) {
        return listAllSecurityGroupInterfaces(session, false, orders);

    }

    @SuppressWarnings("unchecked")
    private static List<SecurityGroupInterface> listAllSecurityGroupInterfaces(Session session,
            boolean userConfigurable, Order[] orders) {
        Criteria criteria = session.createCriteria(SecurityGroupInterface.class)
                .add(Restrictions.eq("isUserConfigurable", userConfigurable))
                .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
        if (orders != null) {
            for (Order order : orders) {
                criteria.addOrder(order);
            }
        }
        return criteria.list();
    }

    public static SecurityGroupInterface findById(Session session, Long id) {
        EntityManager<SecurityGroupInterface> emgr = new EntityManager<SecurityGroupInterface>(
                SecurityGroupInterface.class, session);
        return emgr.findByPrimaryKey(id);
    }

}
