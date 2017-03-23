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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.osc.core.broker.model.entities.appliance.Appliance;
import org.osc.core.broker.model.entities.appliance.ApplianceSoftwareVersion;
import org.osc.core.broker.model.entities.appliance.TagEncapsulationType;
import org.osc.core.broker.model.entities.appliance.VirtualizationType;
import org.osc.core.broker.model.plugin.manager.ManagerType;
import org.osc.core.broker.service.dto.ApplianceModelSoftwareVersionDto;
import org.osc.core.broker.service.dto.ApplianceSoftwareVersionDto;

public class ApplianceSoftwareVersionEntityMgr {

    public static ApplianceSoftwareVersion createEntity(EntityManager em, ApplianceSoftwareVersionDto dto, Appliance a) {

        ApplianceSoftwareVersion av = new ApplianceSoftwareVersion(a);

        toEntity(av, dto);

        return av;

    }

    public static void toEntity(ApplianceSoftwareVersion av, ApplianceSoftwareVersionDto dto) {

        // transfrom from dto to entity
        av.setId(dto.getId());
        av.setApplianceSoftwareVersion(dto.getSwVersion());
        av.setVirtualizationType(VirtualizationType.valueOf(dto.getVirtualizationType().name()));
        av.setVirtualizarionSoftwareVersion(dto.getVirtualizationVersion());
        av.setImageUrl(dto.getImageUrl());
        av.setEncapsulationTypes(dto.getEncapsulationTypes()
                .stream()
                .map(t -> TagEncapsulationType.valueOf(t.name()))
                .collect(Collectors.toList()));
        av.setMinCpus(dto.getMinCpus());
        av.setMemoryInMb(dto.getMemoryInMb());
        av.setDiskSizeInGb(dto.getDiskSizeInGb());
        av.setAdditionalNicForInspection(dto.isAdditionalNicForInspection());
        av.getImageProperties().putAll(dto.getImageProperties());
        av.getConfigProperties().putAll(dto.getConfigProperties());

    }

    public static void fromEntity(ApplianceSoftwareVersion av, ApplianceSoftwareVersionDto dto) {

        // transform from entity to dto
        dto.setId(av.getId());
        dto.setParentId(av.getAppliance().getId());
        dto.setSwVersion(av.getApplianceSoftwareVersion());
        dto.setVirtualizationType(org.osc.core.broker.model.virtualization.VirtualizationType.valueOf(
                av.getVirtualizationType().name()));
        dto.setVirtualizationVersion(av.getVirtualizarionSoftwareVersion());
        dto.setImageUrl(av.getImageUrl());
        dto.setEncapsulationTypes(av.getEncapsulationTypes()
                .stream()
                .map(t -> org.osc.sdk.controller.TagEncapsulationType.valueOf(t.name()))
                .collect(Collectors.toList()));
        dto.setAdditionalNicForInspection(av.hasAdditionalNicForInspection());
    }

    public static ApplianceSoftwareVersion findByApplianceVersionVirtTypeAndVersion(EntityManager em, Long applianceId, String av,
            VirtualizationType vt, String vv) {

        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<ApplianceSoftwareVersion> query = cb.createQuery(ApplianceSoftwareVersion.class);

        Root<ApplianceSoftwareVersion> root = query.from(ApplianceSoftwareVersion.class);

        query = query.select(root)
                .where(cb.equal(root.join("appliance").get("id"), applianceId),
                       cb.equal(cb.upper(root.get("applianceSoftwareVersion")), av.toUpperCase()),
                       cb.equal(root.get("virtualizationType"), vt),
                       cb.equal(cb.upper(root.get("virtualizationSoftwareVersion")), vv.toUpperCase())
                   );

        try {
            return em.createQuery(query).getSingleResult();
        } catch (NoResultException nre) {
            return null;
        }
    }

    public static ApplianceSoftwareVersion findByImageUrl(EntityManager em, String imageUrl) {

        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<ApplianceSoftwareVersion> query = cb.createQuery(ApplianceSoftwareVersion.class);

        Root<ApplianceSoftwareVersion> root = query.from(ApplianceSoftwareVersion.class);

        query = query.select(root)
                .where(cb.equal(root.get("imageUrl"), imageUrl));

        try {
            return em.createQuery(query).getSingleResult();
        } catch (NoResultException nre) {
            return null;
        }
    }

    public static boolean isExisting(EntityManager em, Long applianceId, String applianceSoftwareVersion,
            VirtualizationType virtualizationType, String virtualizationSoftwareVersion) {

        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<ApplianceSoftwareVersion> query = cb.createQuery(ApplianceSoftwareVersion.class);

        Root<ApplianceSoftwareVersion> root = query.from(ApplianceSoftwareVersion.class);

        query = query.select(root)
                .where(cb.equal(root.join("appliance").get("id"), applianceId),
                       cb.equal(cb.upper(root.get("applianceSoftwareVersion")), applianceSoftwareVersion.toUpperCase()),
                       cb.equal(root.get("virtualizationType"), virtualizationType),
                       cb.equal(cb.upper(root.get("virtualizationSoftwareVersion")), virtualizationSoftwareVersion.toUpperCase())
                   );

        List<ApplianceSoftwareVersion> items = em.createQuery(query).setMaxResults(1).getResultList();

        return !items.isEmpty();

    }

    public static boolean isReferencedByApplianceSoftwareVersion(EntityManager em, Appliance a) {

        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<ApplianceSoftwareVersion> query = cb.createQuery(ApplianceSoftwareVersion.class);

        Root<ApplianceSoftwareVersion> root = query.from(ApplianceSoftwareVersion.class);

        query = query.select(root)
                .where(cb.equal(root.join("appliance").get("id"), a.getId()));

        List<ApplianceSoftwareVersion> items = em.createQuery(query).setMaxResults(1).getResultList();

        return !items.isEmpty();
    }

    public static List<ApplianceSoftwareVersion> getApplianceSoftwareVersionsByApplianceId(EntityManager em,
            Long applianceId) {
        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<ApplianceSoftwareVersion> query = cb.createQuery(ApplianceSoftwareVersion.class);

        Root<ApplianceSoftwareVersion> root = query.from(ApplianceSoftwareVersion.class);

        query = query.select(root)
                .where(cb.equal(root.join("appliance").get("id"), applianceId))
                .orderBy(cb.asc(root.get("applianceSoftwareVersion")));

        return em.createQuery(query).getResultList();
    }

    public static List<ApplianceModelSoftwareVersionDto> findByMcType(EntityManager em, ManagerType mcType) {

        String hql = "SELECT DISTINCT A.id AS aId, A.model AS aModel, AV.applianceSoftwareVersion AS avSwVersion FROM Appliance A, ApplianceSoftwareVersion AV"
                + " WHERE A.managerType = :mcType AND AV.appliance.id = A.id"
                + " ORDER BY aModel ASC, avSwVersion DESC";

        TypedQuery<Object[]> query = em.createQuery(hql, Object[].class);
        query.setParameter("mcType", mcType.toString());

        List<Object[]> ls = query.getResultList();

        List<ApplianceModelSoftwareVersionDto> dtoList = new ArrayList<ApplianceModelSoftwareVersionDto>();

        for (Object[] arr : ls) {

            ApplianceModelSoftwareVersionDto dto = new ApplianceModelSoftwareVersionDto();
            dto.setApplianceId((Long) arr[0]);
            dto.setApplianceModel((String) arr[1]);
            dto.setSwVersion((String) arr[2]);
            dto.setName(dto.getApplianceModel() + "-" + dto.getSwVersion());
            dtoList.add(dto);
        }

        return dtoList;

    }

    /**
     * @param em
     *            Hibernate Session
     * @param version
     *            Appliance software version string e.g. "8.2.7.27"
     * @return
     *         List of Appliance Software version objects
     */
    public static List<ApplianceSoftwareVersion> findBSoftwareByVersion(EntityManager em, String version) {
        return new OSCEntityManager<>(ApplianceSoftwareVersion.class, em)
                .listByFieldName("applianceSoftwareVersion", version);
    }

    /**
     * @param em
     *            Hibernate Session
     * @param version
     * @return
     *         List of TagEncapsulationType for a gives Appliance software version
     */
    public static List<TagEncapsulationType> getEncapsulationByApplianceSoftwareVersion(EntityManager em,
            String version, String model, VirtualizationType vcType) {

        List<ApplianceSoftwareVersion> applianceList = findBSoftwareByVersion(em, version);

        for (ApplianceSoftwareVersion av : applianceList) {
            // return list of encapsulation type if the appliance software version is of Type Open stack and Encapsulation Type is not empty
            if (!av.getEncapsulationTypes().isEmpty() && av.getVirtualizationType() == vcType
                    && av.getApplianceModel().equals(model)) {
                return av.getEncapsulationTypes();
            }
        }
        return null;
    }
}
