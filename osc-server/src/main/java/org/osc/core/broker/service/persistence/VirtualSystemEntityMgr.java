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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.apache.commons.collections4.CollectionUtils;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.service.dto.VirtualSystemDto;
import org.osc.core.common.virtualization.VirtualizationType;
import org.osc.sdk.controller.TagEncapsulationType;

public class VirtualSystemEntityMgr {

    public static void fromEntity(VirtualSystem entity, VirtualSystemDto dto) {
        dto.setId(entity.getId());
        dto.setParentId(entity.getDistributedAppliance().getId());
        dto.setName(entity.getName());
        dto.setDistributedAppliance(entity.getDistributedAppliance().getName());
        dto.setVcId(entity.getVirtualizationConnector().getId());
        dto.setVirtualizationConnectorName(entity.getVirtualizationConnector().getName());
        dto.setVirtualizationType(VirtualizationType.valueOf(
                entity.getVirtualizationConnector().getVirtualizationType().name()));
        dto.setMarkForDeletion(entity.getMarkedForDeletion());

        org.osc.core.broker.model.entities.appliance.TagEncapsulationType encapsulationType = entity.getEncapsulationType();

        if(encapsulationType != null) {
            dto.setEncapsulationType(TagEncapsulationType.valueOf(
                    encapsulationType.name()));
        }

        if (entity.getDomain() != null) {
            dto.setDomainId(entity.getDomain().getId());
            dto.setDomainName(entity.getDomain().getName());
        }
    }

    public static VirtualSystem findById(EntityManager em, Long id) {
        return em.find(VirtualSystem.class, id);
    }

    public static VirtualSystem findByDAAndVC(EntityManager em, Long daId, Long vcId) {

        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<VirtualSystem> query = cb.createQuery(VirtualSystem.class);

        Root<VirtualSystem> root = query.from(VirtualSystem.class);

        query = query.select(root)
                .where(cb.equal(root.join("virtualizationConnector").get("id"), vcId),
                        cb.equal(root.join("distributedAppliance").get("id"), daId));

        List<VirtualSystem> list = em.createQuery(query).getResultList();

        if (list == null || list.size() == 0) {
            return null;
        }

        return list.get(0);
    }

    public static List<VirtualSystem> listReferencedVSBySecurityGroup(EntityManager em, Long sgId) {

        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<VirtualSystem> query = cb.createQuery(VirtualSystem.class);

        Root<VirtualSystem> root = query.from(VirtualSystem.class);

        query = query.select(root).distinct(true)
                .where(cb.equal(root.join("securityGroupInterfaces")
                        .join("securityGroup").get("id"), sgId));

        return em.createQuery(query).getResultList();
    }

    public static List<VirtualSystemDto> findByMcApplianceAndSwVer(EntityManager em, Long mcId, Long applianceId,
            String applianceSwVer) {

        String hql = "SELECT VC.name AS vcName, VC.virtualizationType AS vcType, DO.name AS doName FROM ApplianceManagerConnector MC, Appliance A, ApplianceSoftwareVersion AV, VirtualizationConnector VC, Domain DO"
                + " WHERE A.managerType = MC.managerType AND AV.appliance.id = :applianceId AND MC.id = :mcId"
                + " AND AV.applianceSoftwareVersion = :applianceSwVer AND VC.virtualizationSoftwareVersion = AV.virtualizationSoftwareVersion"
                + " AND VC.virtualizationType = AV.virtualizationType AND DO.applianceManagerConnector.id = :mcId"
                + " ORDER BY vcName ASC";

        TypedQuery<Object[]> query = em.createQuery(hql, Object[].class);
        query.setParameter("mcId", mcId);
        query.setParameter("applianceId", applianceId);
        query.setParameter("applianceSwVer", applianceSwVer);

        List<Object[]> ls = query.getResultList();

        List<VirtualSystemDto> dtoList = new ArrayList<VirtualSystemDto>();

        for (Object[] arr : ls) {

            VirtualSystemDto dto = new VirtualSystemDto();

            dto.setVirtualizationConnectorName((String) arr[0]);
            dto.setVirtualizationType((VirtualizationType) arr[1]);
            dto.setDomainName((String) arr[2]);

            dtoList.add(dto);
        }

        return dtoList;

    }

    public static List<Long> listByVcId(EntityManager em, Long vcId) {

        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<Long> query = cb.createQuery(Long.class);

        Root<VirtualSystem> root = query.from(VirtualSystem.class);

        query = query.select(root.get("id")).distinct(true)
                .where(cb.equal(root.join("virtualizationConnector").get("id"), vcId));

        List<Long> list = em.createQuery(query).getResultList();

        if (list == null || list.size() == 0) {
            return null;
        }

        return list;
    }

    /**
     * List all tags used within a VS and locate the next 'minimum' available tag starting with 2. If there are tags
     * 'holes' (i.e. for "1,2,3,6,7,9" - 4,5,8,10... will be available for allocation). If no 'holes' available,
     * will allocate the next minimum number (10 in our example).
     *
     * @param session
     *            database session
     * @param vs
     *            Virtual System Object to get tag for
     * @return Minimum and unique tag for given VS.
     */
    @SuppressWarnings("unchecked")
    public static synchronized Long generateUniqueTag(EntityManager em, VirtualSystem vs) {
        vs = em.find(VirtualSystem.class, vs.getId(),
                LockModeType.PESSIMISTIC_WRITE);
        String sql = "SELECT CONVERT(SUBSTR(tag,LOCATE('-',tag)+1), LONG) AS tag_val "
                + "FROM security_group_interface WHERE virtual_system_fk = " + vs.getId() + " ORDER BY tag_val";
        List<Object> list = em.createNativeQuery(sql).getResultList();
        // Start with 2 as 1 is reserved in some cases
        // TODO: arvindn - Some security partners require tag's larger than 300. Remove once problem is fixed on the
        // partners side.
        Long prevVal = 301L;
        for (Object tag : list) {
            long tagValue = ((BigInteger) tag).longValue();
            if (tagValue != prevVal) {
                return prevVal;
            }
            prevVal++;
        }
        return prevVal;
    }

	/**
	 * @param VirtualSystem
	 * @return true if ServiceFunctionChain(SFC) is bind with SecurityGroup(SG)
	 */
    public static boolean isProtectingWorkload(VirtualSystem vs) {
        return CollectionUtils.emptyIfNull(vs.getDeploymentSpecs()).stream()
            .anyMatch(ds -> DeploymentSpecEntityMgr.isProtectingWorkload(ds));
    }
}
