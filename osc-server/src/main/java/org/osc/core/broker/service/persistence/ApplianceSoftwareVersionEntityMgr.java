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

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.osc.core.broker.model.entities.appliance.Appliance;
import org.osc.core.broker.model.entities.appliance.ApplianceSoftwareVersion;
import org.osc.core.broker.model.entities.appliance.TagEncapsulationType;
import org.osc.core.broker.model.entities.appliance.VirtualizationType;
import org.osc.core.broker.model.plugin.manager.ManagerType;
import org.osc.core.broker.service.dto.ApplianceModelSoftwareVersionDto;
import org.osc.core.broker.service.dto.ApplianceSoftwareVersionDto;
//import org.osc.sdk.controller.TagEncapsulationType;

public class ApplianceSoftwareVersionEntityMgr {

    public static ApplianceSoftwareVersion createEntity(Session session, ApplianceSoftwareVersionDto dto, Appliance a) {

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

    public static ApplianceSoftwareVersion findByApplianceVersionVirtTypeAndVersion(Session session, Long applianceId, String av,
            VirtualizationType vt, String vv) {

        Criteria criteria = session.createCriteria(ApplianceSoftwareVersion.class).createAlias("appliance", "a")
                .add(Restrictions.eq("a.id", applianceId))
                .add(Restrictions.eq("applianceSoftwareVersion", av).ignoreCase())
                .add(Restrictions.eq("virtualizationType", vt))
                .add(Restrictions.eq("virtualizationSoftwareVersion", vv).ignoreCase());

        ApplianceSoftwareVersion item = (ApplianceSoftwareVersion) criteria.setFirstResult(0).setMaxResults(1)
                .uniqueResult();

        return item;

    }

    public static ApplianceSoftwareVersion findByImageUrl(Session session, String imageUrl) {

        Criteria criteria = session.createCriteria(ApplianceSoftwareVersion.class)
                .add(Restrictions.eq("imageUrl", imageUrl));

        ApplianceSoftwareVersion item = (ApplianceSoftwareVersion) criteria.setFirstResult(0).setMaxResults(1)
                .uniqueResult();

        return item;

    }

    public static boolean isExisting(Session session, Long applianceId, String applianceSoftwareVersion,
            VirtualizationType virtualizationType, String virtualizationSoftwareVersion) {

        Criteria criteria = session.createCriteria(ApplianceSoftwareVersion.class).createAlias("appliance", "a")
                .add(Restrictions.eq("a.id", applianceId))
                .add(Restrictions.eq("applianceSoftwareVersion", applianceSoftwareVersion).ignoreCase())
                .add(Restrictions.eq("virtualizationType", virtualizationType))
                .add(Restrictions.eq("virtualizationSoftwareVersion", virtualizationSoftwareVersion).ignoreCase());

        Long count = (Long) criteria.setProjection(Projections.rowCount()).setFirstResult(0).setMaxResults(1)
                .uniqueResult();

        if (count > 0) {

            return true;
        }

        return false;

    }

    public static boolean isReferencedByApplianceSoftwareVersion(Session session, Appliance a) {

        Criteria criteria = session.createCriteria(ApplianceSoftwareVersion.class).createAlias("appliance", "a")
                .add(Restrictions.eq("a.id", a.getId()));

        Long count = (Long) criteria.setProjection(Projections.rowCount()).setFirstResult(0).setMaxResults(1)
                .uniqueResult();

        if (count > 0) {

            return true;
        }

        return false;

    }

    @SuppressWarnings("unchecked")
    public static List<ApplianceSoftwareVersion> getApplianceSoftwareVersionsByApplianceId(Session session,
            Long applianceId) {
        return session.createCriteria(ApplianceSoftwareVersion.class).add(Restrictions.eq("appliance.id", applianceId))
                .addOrder(Order.asc("applianceSoftwareVersion")).list();
    }

    @SuppressWarnings("unchecked")
    public static List<ApplianceModelSoftwareVersionDto> findByMcType(Session session, ManagerType mcType) {

        String hql = "SELECT DISTINCT A.id AS aId, A.model AS aModel, AV.applianceSoftwareVersion AS avSwVersion FROM Appliance A, ApplianceSoftwareVersion AV"
                + " WHERE A.managerType = :mcType AND AV.appliance.id = A.id"
                + " ORDER BY aModel ASC, avSwVersion DESC";

        Query query = session.createQuery(hql);
        query.setParameter("mcType", mcType.toString());

        List<Object[]> ls = query.list();

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
     * @param session
     *            Hibernate Session
     * @param version
     *            Appliance software version string e.g. "8.2.7.27"
     * @return
     *         List of Appliance Software version objects
     */
    @SuppressWarnings("unchecked")
    public static List<ApplianceSoftwareVersion> findBSoftwareByVersion(Session session, String version) {
        Criteria criteria = session.createCriteria(ApplianceSoftwareVersion.class)
                .add(Restrictions.eq("applianceSoftwareVersion", version))
                .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
        return criteria.list();

    }

    /**
     * @param session
     *            Hibernate Session
     * @param version
     * @return
     *         List of TagEncapsulationType for a gives Appliance software version
     */
    public static List<TagEncapsulationType> getEncapsulationByApplianceSoftwareVersion(Session session,
            String version, String model, VirtualizationType vcType) {

        List<ApplianceSoftwareVersion> applianceList = findBSoftwareByVersion(session, version);

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
