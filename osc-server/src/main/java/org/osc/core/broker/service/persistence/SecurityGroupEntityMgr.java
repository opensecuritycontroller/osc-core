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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.osc.core.broker.model.entities.appliance.DistributedAppliance;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMember;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMemberType;
import org.osc.core.broker.model.entities.virtualization.openstack.VMPort;
import org.osc.core.broker.service.securitygroup.SecurityGroupDto;

public class SecurityGroupEntityMgr {

    public static void toEntity(SecurityGroup entity, SecurityGroupDto dto) {
        entity.setName(dto.getName());
        entity.setTenantName(dto.getTenantName());
        entity.setProtectAll(dto.isProtectAll());
    }

    /**
     * This method does not include security group members of the security group
     * 
     */
    public static void fromEntity(SecurityGroup entity, SecurityGroupDto dto) {
        dto.setId(entity.getId());
        dto.setParentId(entity.getVirtualizationConnector().getId());
        dto.setName(entity.getName());
        dto.setMarkForDeletion(entity.getMarkedForDeletion());
        dto.setProtectAll(entity.isProtectAll());
        dto.setVirtualizationConnectorName(entity.getVirtualizationConnector().getName());
        dto.setTenantId(entity.getTenantId());
        dto.setTenantName(entity.getTenantName());
        if (entity.getLastJob() != null) {
            dto.setLastJobStatus(entity.getLastJob().getStatus());
            dto.setLastJobState(entity.getLastJob().getState());
            dto.setLastJobId(entity.getLastJob().getId());
        }
    }

    @SuppressWarnings("unchecked")
    public static SecurityGroupDto generateDescription(Session session, SecurityGroupDto dto) {
        Criteria serviceNameCriteria = session.createCriteria(DistributedAppliance.class, "da")
                .createAlias("da.virtualSystems", "vs").createAlias("vs.securityGroupInterfaces", "sgi")
                .createAlias("sgi.securityGroups", "sg").add(Restrictions.eq("sg.id", dto.getId()))
                .setProjection(Projections.property("name")).addOrder(Order.asc("sgi.order"));
        List<String> serviceNames = serviceNameCriteria.list();

        dto.setServicesDescription(StringUtils.join(serviceNames, ","));

        SecurityGroup sg = findById(session, dto.getId());

        if (sg.getVirtualizationConnector().isOpenstack()) {

            Criteria vmNumberCriteria = session.createCriteria(SecurityGroupMember.class, "sgm")
                    .createAlias("sgm.securityGroup", "sg").add(Restrictions.eq("sg.id", dto.getId()))
                    .setProjection(Projections.rowCount()).add(Restrictions.eq("type", SecurityGroupMemberType.VM));

            Criteria nwNumberCriteria = session.createCriteria(SecurityGroupMember.class, "sgm")
                    .createAlias("sgm.securityGroup", "sg").add(Restrictions.eq("sg.id", dto.getId()))
                    .setProjection(Projections.rowCount())
                    .add(Restrictions.eq("type", SecurityGroupMemberType.NETWORK));

            Criteria subNumberCriteria = session.createCriteria(SecurityGroupMember.class, "sgm")
                    .createAlias("sgm.securityGroup", "sg").add(Restrictions.eq("sg.id", dto.getId()))
                    .setProjection(Projections.rowCount()).add(Restrictions.eq("type", SecurityGroupMemberType.SUBNET));

            dto.setMemberDescription(String.format("VM: %d , Network: %d , Subnet: %d",
                    vmNumberCriteria.uniqueResult(), nwNumberCriteria.uniqueResult(), subNumberCriteria.uniqueResult()));

        } else if (sg.getVirtualizationConnector().isVmware()) {

            Criteria ipNumberCriteria = session.createCriteria(SecurityGroupMember.class, "sgm")
                    .createAlias("sgm.securityGroup", "sg").add(Restrictions.eq("sg.id", dto.getId()))
                    .setProjection(Projections.rowCount()).add(Restrictions.eq("type", SecurityGroupMemberType.IP));
            Criteria macNumberCriteria = session.createCriteria(SecurityGroupMember.class, "sgm")
                    .createAlias("sgm.securityGroup", "sg").add(Restrictions.eq("sg.id", dto.getId()))
                    .setProjection(Projections.rowCount()).add(Restrictions.eq("type", SecurityGroupMemberType.MAC));

            dto.setMemberDescription(String.format("IP: %d , Mac: %d ", ipNumberCriteria.uniqueResult(),
                    macNumberCriteria.uniqueResult()));

        }

        return dto;
    }

    @SuppressWarnings("unchecked")
    public static List<SecurityGroup> listSecurityGroupsByVcId(Session session, Long vcId) {
        Criteria criteria = session.createCriteria(SecurityGroup.class).createAlias("virtualizationConnector", "vc")
                .addOrder(Order.asc("name")).add(Restrictions.eq("vc.id", vcId))
                .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
        return criteria.list();
    }

    public static SecurityGroup listSecurityGroupsByVcIdAndMgrId(Session session, Long vcId, String mgrId) {
        Criteria criteria = session.createCriteria(SecurityGroup.class).createAlias("virtualizationConnector", "vc")
                .add(Restrictions.eq("vc.id", vcId)).add(Restrictions.eq("mgrId", mgrId));

        return (SecurityGroup) criteria.uniqueResult();
    }

    @SuppressWarnings("unchecked")
    public static List<SecurityGroup> listSecurityGroupsByVsAndNoBindings(Session session, VirtualSystem vs) {
        Criteria criteria = session.createCriteria(SecurityGroup.class).createAlias("virtualizationConnector", "vc")
                .add(Restrictions.eq("vc.id", vs.getVirtualizationConnector().getId()))
                .add(Restrictions.isEmpty("securityGroupInterfaces"))
                .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
        return criteria.list();
    }

    /**
     * @param session
     *            Hibernate Session
     * @param sg
     *            Security Group
     * @return
     *         true if a new SGM is added.
     *         false if no new SGM was added
     */
    public static boolean hasNewSGM(Session session, SecurityGroup sg) {

        // VM
        Criteria vmCriteria = session.createCriteria(SecurityGroup.class).createAlias("securityGroupMembers", "sgm")
                .add(Restrictions.eq("sgm.type", SecurityGroupMemberType.VM)).createAlias("sgm.vm", "vm")
                .add(Restrictions.isNull("vm.host")).setProjection(Projections.id()).setMaxResults(1);

        if (vmCriteria.uniqueResult() != null) {
            return true;
        }

        // NW
        Criteria nwCriteria = session.createCriteria(SecurityGroup.class).createAlias("securityGroupMembers", "sgm")
                .add(Restrictions.eq("sgm.type", SecurityGroupMemberType.NETWORK))
                .createAlias("sgm.network", "network").add(Restrictions.isEmpty("network.ports"))
                .setProjection(Projections.id()).setMaxResults(1);

        if (nwCriteria.uniqueResult() != null) {
            return true;
        }

        // Subnet
        Criteria subCriteria = session.createCriteria(SecurityGroup.class).createAlias("securityGroupMembers", "sgm")
                .add(Restrictions.eq("sgm.type", SecurityGroupMemberType.SUBNET)).createAlias("sgm.subnet", "subnet")
                .add(Restrictions.isEmpty("subnet.ports")).setProjection(Projections.id()).setMaxResults(1);

        if (subCriteria.uniqueResult() != null) {
            return true;
        }

        return false;
    }

    /**
     * @param session
     *            Hibernate Session
     * @param sg
     *            Security Group
     * @return
     *         true if any SGM was deleted
     *         false if no SGM was deleted
     */
    public static boolean hasSGMRemoved(Session session, SecurityGroup sg) {
        Criteria criteria = session.createCriteria(SecurityGroup.class).createAlias("securityGroupMembers", "sgm")
                .add(Restrictions.eq("sgm.markedForDeletion", true)).setProjection(Projections.id()).setMaxResults(1);

        if (criteria.uniqueResult() != null) {
            return true;
        }

        return false;
    }

    public static boolean isSecurityGroupExistWithProtectAll(Session session, String tenantId, Long vcId) {
        Criteria criteria = session.createCriteria(SecurityGroup.class).createAlias("virtualizationConnector", "vc")
                .add(Restrictions.eq("tenantId", tenantId)).add(Restrictions.eq("vc.id", vcId))
                .add(Restrictions.eq("protectAll", true)).setProjection(Projections.id()).setMaxResults(1);

        if (criteria.uniqueResult() != null) {
            return true;
        }
        return false;
    }

    public static boolean isSecurityGroupExistWithSameNameAndTenant(Session session, String name, String tenantId) {
        Criteria criteria = session.createCriteria(SecurityGroup.class).add(Restrictions.eq("tenantId", tenantId))
                .add(Restrictions.eq("name", name)).setProjection(Projections.id()).setMaxResults(1);

        if (criteria.uniqueResult() != null) {
            return true;
        }
        return false;

    }

    public static SecurityGroup findById(Session session, Long id) {

        // Initializing Entity Manager
        EntityManager<SecurityGroup> emgr = new EntityManager<SecurityGroup>(SecurityGroup.class, session);

        return emgr.findByPrimaryKey(id);
    }

    @SuppressWarnings("unchecked")
    public static List<SecurityGroup> findByNsxServiceProfileIdAndVs(Session session, VirtualSystem vs,
            String serviceProfileId) {
        Criteria criteria = session.createCriteria(SecurityGroup.class, "sg")
                .createAlias("sg.securityGroupInterfaces", "sgi").add(Restrictions.eq("sgi.tag", serviceProfileId))
                .add(Restrictions.eq("sgi.virtualSystem", vs)).setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
        return criteria.list();
    }

    @SuppressWarnings("unchecked")
    public static List<SecurityGroup> listByProtectAllAndtenantId(Session session, String tenantId) {
        Criteria criteria = session.createCriteria(SecurityGroup.class).add(Restrictions.eq("protectAll", true))
                .add(Restrictions.eq("tenantId", tenantId)).setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
        return criteria.list();
    }

    @SuppressWarnings("unchecked")
    public static List<SecurityGroup> listByTenantId(Session session, String tenantId) {
        Criteria criteria = session.createCriteria(SecurityGroup.class).add(Restrictions.eq("tenantId", tenantId));
        criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
        return criteria.list();
    }

    // List SG by Port or Network Id...
    @SuppressWarnings("unchecked")
    public static List<SecurityGroup> listByNetworkId(Session session, String sgId, String networkId) {

        // get Network or VM from port ID then Verify SGM ID and get GD ID...

        Criteria criteria = session.createCriteria(SecurityGroup.class, "sg").add(Restrictions.eq("id", sgId))
                .createAlias("sg.securityGroupMembers.network", "network")
                .add(Restrictions.eq("network.openstackId", networkId))
                .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
        return criteria.list();
    }

    @SuppressWarnings("unchecked")
    public static List<SecurityGroup> listByDeviceId(Session session, String sgId, String deviceId) {
        Criteria criteria = session.createCriteria(SecurityGroup.class, "sg").add(Restrictions.eq("id", sgId))
                .createAlias("sg.securityGroupMembers", "sgm").add(Restrictions.eq("sgm.openstackId", deviceId))
                .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
        return criteria.list();
    }

    public static Set<SecurityGroup> listByDai(Session session, DistributedApplianceInstance dai) {
        Set<SecurityGroup> sgs = new HashSet<SecurityGroup>();
        for (VMPort port : dai.getProtectedPorts()) {
            if (port.getVm() != null) {
                for (SecurityGroupMember sgm : port.getVm().getSecurityGroupMembers()) {
                    sgs.add(sgm.getSecurityGroup());
                }
            }
            if (port.getNetwork() != null) {
                for (SecurityGroupMember sgm : port.getNetwork().getSecurityGroupMembers()) {
                    sgs.add(sgm.getSecurityGroup());
                }
            }
        }

        return sgs;
    }
}
