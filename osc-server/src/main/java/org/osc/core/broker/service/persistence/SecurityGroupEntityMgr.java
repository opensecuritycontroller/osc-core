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

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Root;

import org.apache.commons.lang.StringUtils;
import org.osc.core.broker.model.entities.appliance.DistributedAppliance;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.job.JobRecord;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupInterface;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMember;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMemberType;
import org.osc.core.broker.model.entities.virtualization.ServiceFunctionChain;
import org.osc.core.broker.model.entities.virtualization.openstack.VMPort;
import org.osc.core.broker.service.dto.SecurityGroupDto;
import org.osc.core.common.virtualization.VirtualizationType;

public class SecurityGroupEntityMgr {

    public static void toEntity(SecurityGroup entity, SecurityGroupDto dto) {
        entity.setName(dto.getName());
        entity.setProjectName(dto.getProjectName());
        entity.setProtectAll(dto.isProtectAll());
    }

    /**
     * This method does not include security group members of the security group
     *
     */
    public static void fromEntity(SecurityGroup entity, SecurityGroupDto dto) {
        ServiceFunctionChain sfc = entity.getServiceFunctionChain();

        dto.setId(entity.getId());
        dto.setParentId(entity.getVirtualizationConnector().getId());
        dto.setName(entity.getName());
        dto.setMarkForDeletion(entity.getMarkedForDeletion());
        dto.setProtectAll(entity.isProtectAll());
        dto.setVirtualizationConnectorName(entity.getVirtualizationConnector().getName());
        dto.setProjectId(entity.getProjectId());
        dto.setProjectName(entity.getProjectName());
        dto.setServiceFunctionChainId(sfc == null ? null : sfc.getId());
        JobRecord lastJob = entity.getLastJob();
        if (lastJob != null) {
            dto.setLastJobStatus(lastJob.getStatus().name());
            dto.setLastJobState(lastJob.getState().name());
            dto.setLastJobId(lastJob.getId());
        }
    }

    public static SecurityGroupDto generateDescription(EntityManager em, SecurityGroupDto dto) {
        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<String> query = cb.createQuery(String.class);

        Root<DistributedAppliance> root = query.from(DistributedAppliance.class);
        Join<DistributedAppliance, SecurityGroupInterface> sgi = root.join("virtualSystems").join("securityGroupInterfaces");
        Join<SecurityGroupInterface, SecurityGroup> sgEntity = sgi.join("securityGroup");

        query = query.select(root.get("name"))
                .where(cb.equal(sgEntity.get("id"), dto.getId()))
                .orderBy(cb.asc(sgi.get("order")));

        List<String> serviceNames = em.createQuery(query).getResultList();

        dto.setServicesDescription(StringUtils.join(serviceNames, ","));

        SecurityGroup sg = findById(em, dto.getId());

        if (sg.getVirtualizationConnector().getVirtualizationType() == VirtualizationType.OPENSTACK) {

            CriteriaQuery<Long> vmNumberCriteria = cb.createQuery(Long.class);

            Root<SecurityGroupMember> vmRoot = vmNumberCriteria.from(SecurityGroupMember.class);
            vmNumberCriteria = vmNumberCriteria.select(cb.count(vmRoot))
                    .where(cb.equal(vmRoot.join("securityGroup").get("id"), dto.getId()),
                            cb.equal(vmRoot.get("type"), SecurityGroupMemberType.VM));

            CriteriaQuery<Long> nwNumberCriteria = cb.createQuery(Long.class);

            Root<SecurityGroupMember> nwRoot = nwNumberCriteria.from(SecurityGroupMember.class);
            nwNumberCriteria = nwNumberCriteria.select(cb.count(nwRoot))
                    .where(cb.equal(nwRoot.join("securityGroup").get("id"), dto.getId()),
                            cb.equal(nwRoot.get("type"), SecurityGroupMemberType.NETWORK));

            CriteriaQuery<Long> subNumberCriteria = cb.createQuery(Long.class);

            Root<SecurityGroupMember> subRoot = subNumberCriteria.from(SecurityGroupMember.class);
            subNumberCriteria = subNumberCriteria.select(cb.count(subRoot))
                    .where(cb.equal(subRoot.join("securityGroup").get("id"), dto.getId()),
                            cb.equal(subRoot.get("type"), SecurityGroupMemberType.SUBNET));


            dto.setMemberDescription(String.format("VM: %d , Network: %d , Subnet: %d",
                    em.createQuery(vmNumberCriteria).getSingleResult(),
                    em.createQuery(nwNumberCriteria).getSingleResult(),
                    em.createQuery(subNumberCriteria).getSingleResult()));

        }

        return dto;
    }

	public static List<SecurityGroup> listOtherSecurityGroupsWithSameSFC(EntityManager em,
			SecurityGroup sg) {
		CriteriaBuilder cb = em.getCriteriaBuilder();

		CriteriaQuery<SecurityGroup> query = cb.createQuery(SecurityGroup.class);

		Root<SecurityGroup> root = query.from(SecurityGroup.class);
		query = query.select(root).where(cb.equal(root.join("serviceFunctionChain"), sg.getServiceFunctionChain()),
				cb.equal(root.get("projectId"), sg.getProjectId()), cb.notEqual(root, sg));

		List<SecurityGroup> list = em.createQuery(query).getResultList();
		return list;
	}

	public static List<SecurityGroup> listOtherSecurityGroupsWithSameNetworkElementID(EntityManager em,
			SecurityGroup sg) {
		CriteriaBuilder cb = em.getCriteriaBuilder();

		CriteriaQuery<SecurityGroup> query = cb.createQuery(SecurityGroup.class);

		Root<SecurityGroup> root = query.from(SecurityGroup.class);
		query = query.select(root).where(cb.equal(root.get("networkElementId"), sg.getNetworkElementId()),
				cb.equal(root.get("projectId"), sg.getProjectId()), cb.notEqual(root, sg));

		List<SecurityGroup> list = em.createQuery(query).getResultList();
		return list;
	}

    public static List<SecurityGroup> listSecurityGroupsByVcId(EntityManager em, Long vcId) {
        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<SecurityGroup> query = cb.createQuery(SecurityGroup.class);

        Root<SecurityGroup> root = query.from(SecurityGroup.class);
        query = query.select(root).distinct(true)
                .where(cb.equal(root.join("virtualizationConnector").get("id"), vcId))
                .orderBy(cb.asc(root.get("name")));

        return em.createQuery(query).getResultList();
    }

    public static SecurityGroup listSecurityGroupsByVcIdAndMgrId(EntityManager em, Long vcId, String mgrId) {
        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<SecurityGroup> query = cb.createQuery(SecurityGroup.class);

        Root<SecurityGroup> root = query.from(SecurityGroup.class);
        query = query.select(root)
                .where(cb.equal(root.join("virtualizationConnector").get("id"), vcId),
                        cb.equal(root.join("securityGroupInterfaces").get("mgrSecurityGroupId"), mgrId))
                .orderBy(cb.asc(root.get("name")));

        try {
            return em.createQuery(query).getSingleResult();
        } catch (NoResultException nre) {
            return null;
        }
    }

    public static List<SecurityGroup> listSecurityGroupsByVsAndNoBindings(EntityManager em, VirtualSystem vs) {
        Long vcId = vs.getVirtualizationConnector().getId();

        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<SecurityGroup> query = cb.createQuery(SecurityGroup.class);

        Root<SecurityGroup> root = query.from(SecurityGroup.class);
        query = query.select(root).distinct(true)
                .where(cb.equal(root.join("virtualizationConnector").get("id"), vcId),
                        cb.isEmpty(root.get("securityGroupInterfaces")))
                .orderBy(cb.asc(root.get("name")));

        return em.createQuery(query).getResultList();
    }

    public static List<SecurityGroup>  listSecurityGroupsBySfcIdAndProjectId(EntityManager em, Long sfcId, String projectId) {
        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<SecurityGroup> query = cb.createQuery(SecurityGroup.class);

        Root<SecurityGroup> root = query.from(SecurityGroup.class);
        query = query.select(root)
                .where(cb.equal(root.join("serviceFunctionChain").get("id"), sfcId),
                        cb.equal(root.get("projectId"), projectId));

        return em.createQuery(query).getResultList();
    }

    /**
     * @param em
     *            Hibernate EntityManager
     * @param sg
     *            Security Group
     * @return
     *         true if a new SGM is added.
     *         false if no new SGM was added
     */
    public static boolean hasNewSGM(EntityManager em, SecurityGroup sg) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<SecurityGroup> query;
        Root<SecurityGroup> root;
        Join<SecurityGroup, Object> join;
        Join<Object, Object> join2;

        // VM
        query = cb.createQuery(SecurityGroup.class);

        root = query.from(SecurityGroup.class);
        join = root.join("securityGroupMembers");
        join2 = join.join("vm");
        query = query.select(root)
                .distinct(true)
                .where(cb.and(
                        cb.equal(join.get("type"), SecurityGroupMemberType.VM),
                        cb.isNull(join2.get("host"))));

        if(!em.createQuery(query).setMaxResults(1).getResultList().isEmpty()) {
            return true;
        }

        // NW
        query = cb.createQuery(SecurityGroup.class);

        root = query.from(SecurityGroup.class);
        join = root.join("securityGroupMembers");
        join2 = join.join("network");
        query = query.select(root)
                .distinct(true)
                .where(cb.and(
                        cb.equal(join.get("type"), SecurityGroupMemberType.NETWORK),
                        cb.isEmpty(join2.get("ports"))));

        if(!em.createQuery(query).setMaxResults(1).getResultList().isEmpty()) {
            return true;
        }

        // Subnet
        query = cb.createQuery(SecurityGroup.class);

        root = query.from(SecurityGroup.class);
        join = root.join("securityGroupMembers");
        join2 = join.join("subnet");
        query = query.select(root)
                .distinct(true)
                .where(cb.and(
                        cb.equal(join.get("type"), SecurityGroupMemberType.SUBNET),
                        cb.isEmpty(join2.get("ports"))));

        if(!em.createQuery(query).setMaxResults(1).getResultList().isEmpty()) {
            return true;
        }

        return false;
    }

    /**
     * @param em
     *            Hibernate EntityManager
     * @param sg
     *            Security Group
     * @return
     *         true if any SGM was deleted
     *         false if no SGM was deleted
     */
    public static boolean hasSGMRemoved(EntityManager em, SecurityGroup sg) {
        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<SecurityGroup> query = cb.createQuery(SecurityGroup.class);

        Root<SecurityGroup> root = query.from(SecurityGroup.class);
        Join<SecurityGroup, Object> join = root.join("securityGroupMembers");
        query = query.select(root)
                .distinct(true)
                .where(cb.equal(join.get("markedForDeletion"), true));

        return !em.createQuery(query).setMaxResults(1).getResultList().isEmpty();
    }

    public static boolean isSecurityGroupExistWithProtectAll(EntityManager em, String projectId, Long vcId) {
        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<SecurityGroup> query = cb.createQuery(SecurityGroup.class);

        Root<SecurityGroup> root = query.from(SecurityGroup.class);
        Join<SecurityGroup, Object> join = root.join("virtualizationConnector");
        query = query.select(root)
                .distinct(true)
                .where(cb.and(
                        cb.equal(root.get("projectId"), projectId),
                        cb.equal(root.get("protectAll"), true),
                        cb.equal(join.get("id"), vcId)));

        return !em.createQuery(query).setMaxResults(1).getResultList().isEmpty();
    }

    public static boolean isSecurityGroupExistWithSameNameAndProject(EntityManager em, String name, String projectId) {
        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<SecurityGroup> query = cb.createQuery(SecurityGroup.class);

        Root<SecurityGroup> root = query.from(SecurityGroup.class);
        query = query.select(root)
                .distinct(true)
                .where(cb.and(
                        cb.equal(root.get("projectId"), projectId),
                        cb.equal(root.get("name"), name)));

        return !em.createQuery(query).setMaxResults(1).getResultList().isEmpty();
    }

    public static SecurityGroup findById(EntityManager em, Long id) {
        return em.find(SecurityGroup.class, id);
    }

    public static List<SecurityGroup> listByProtectAllAndProjectId(EntityManager em, String projectId) {
        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<SecurityGroup> query = cb.createQuery(SecurityGroup.class);

        Root<SecurityGroup> root = query.from(SecurityGroup.class);
        query = query.select(root)
                .distinct(true)
                .where(cb.and(
                        cb.equal(root.get("projectId"), projectId),
                        cb.equal(root.get("protectAll"), true)));

        return em.createQuery(query).getResultList();
    }

    public static List<SecurityGroup> listByProjectId(EntityManager em, String projectId) {
        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<SecurityGroup> query = cb.createQuery(SecurityGroup.class);

        Root<SecurityGroup> root = query.from(SecurityGroup.class);
        query = query.select(root)
                .distinct(true)
                .where(cb.equal(root.get("projectId"), projectId));

        return em.createQuery(query).getResultList();
    }

    // List SG by Port or Network Id...
    public static List<SecurityGroup> listByNetworkId(EntityManager em, String sgId, String networkId) {

        // get Network or VM from port ID then Verify SGM ID and get GD ID...

        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<SecurityGroup> query = cb.createQuery(SecurityGroup.class);

        Root<SecurityGroup> root = query.from(SecurityGroup.class);
        Join<SecurityGroup, Object> join = root.join("securityGroupMembers");
        query = query.select(root)
                .distinct(true)
                .where(cb.and(
                        cb.equal(root.get("id"), sgId),
                        cb.equal(join.get("network").get("openstackId"), networkId)));

        return em.createQuery(query).getResultList();
    }

    public static List<SecurityGroup> listByDeviceId(EntityManager em, String sgId, String deviceId) {
        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<SecurityGroup> query = cb.createQuery(SecurityGroup.class);

        Root<SecurityGroup> root = query.from(SecurityGroup.class);
        Join<SecurityGroup, Object> join = root.join("securityGroupMembers");
        query = query.select(root)
                .distinct(true)
                .where(cb.and(
                        cb.equal(root.get("id"), sgId),
                        cb.equal(join.get("openstackId"), deviceId)));

        return em.createQuery(query).getResultList();
    }

    @SuppressWarnings("unchecked")
    public static Set<SecurityGroup> listByDai(EntityManager em, DistributedApplianceInstance dai) {
        Set<SecurityGroup> sgs = new HashSet<>();
        for (VMPort port : (Set<VMPort>) dai.getProtectedPorts()) {
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
