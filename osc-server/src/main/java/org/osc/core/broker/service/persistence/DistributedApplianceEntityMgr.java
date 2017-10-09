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
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.apache.commons.collections4.CollectionUtils;
import org.osc.core.broker.model.entities.appliance.Appliance;
import org.osc.core.broker.model.entities.appliance.ApplianceSoftwareVersion;
import org.osc.core.broker.model.entities.appliance.DistributedAppliance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.service.api.server.EncryptionApi;
import org.osc.core.broker.service.api.server.EncryptionException;
import org.osc.core.broker.service.dto.DistributedApplianceDto;
import org.osc.core.broker.service.dto.VirtualSystemDto;

public class DistributedApplianceEntityMgr {

    public static DistributedAppliance createEntity(EntityManager em, DistributedApplianceDto dto, Appliance a,
            DistributedAppliance da, EncryptionApi encrypter) throws EncryptionException {

        toEntity(a, da, dto, encrypter);

        return da;

    }

    public static void toEntity(Appliance a, DistributedAppliance da, DistributedApplianceDto dto,
            EncryptionApi encrypter) throws EncryptionException {

        // transform from dto to entity
        da.setId(dto.getId());
        da.setAppliance(a);
        da.setName(dto.getName());
        da.setMgrSecretKey(encrypter.encryptAESCTR(dto.getSecretKey()));
        da.setApplianceVersion(dto.getApplianceSoftwareVersionName());
    }

    public static void fromEntity(DistributedAppliance da, DistributedApplianceDto dto, EncryptionApi encrypter) throws EncryptionException {

        // transform from entity to dto
        dto.setId(da.getId());
        dto.setName(da.getName());
        dto.setApplianceManagerConnectorName(da.getApplianceManagerConnector().getName());
        dto.setMcId(da.getApplianceManagerConnector().getId());
        if (da.getLastJob() != null) {
            dto.setLastJobStatus(da.getLastJob().getStatus().name());
            dto.setLastJobState(da.getLastJob().getState().name());
            dto.setLastJobId(da.getLastJob().getId());
        }
        dto.setSecretKey(encrypter.decryptAESCTR(da.getMgrSecretKey()));
        dto.setApplianceId(da.getAppliance().getId());
        dto.setApplianceModel(da.getAppliance().getModel());
        dto.setApplianceSoftwareVersionName(da.getApplianceVersion());
        dto.setMarkForDeletion(da.getMarkedForDeletion());

        // build the Virtualization System List
        Set<VirtualSystemDto> ls = new HashSet<VirtualSystemDto>();

        for (VirtualSystem vs : da.getVirtualSystems()) {

            VirtualSystemDto vsDto = new VirtualSystemDto();
            VirtualSystemEntityMgr.fromEntity(vs, vsDto);

            ls.add(vsDto);
        }

        dto.setVirtualizationSystems(ls);
    }

    public static List<DistributedAppliance> listAllActive(EntityManager em) {
        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<DistributedAppliance> query = cb.createQuery(DistributedAppliance.class);

        Root<DistributedAppliance> root = query.from(DistributedAppliance.class);

        query = query.select(root).distinct(true)
                .where(cb.equal(root.get("markedForDeletion"), false));

        return em.createQuery(query).getResultList();
    }

    public static List<DistributedAppliance> listActiveByManagerConnector(EntityManager em, ApplianceManagerConnector mc) {
        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<DistributedAppliance> query = cb.createQuery(DistributedAppliance.class);

        Root<DistributedAppliance> root = query.from(DistributedAppliance.class);

        query = query.select(root).distinct(true)
                .where(cb.equal(root.get("markedForDeletion"), false),
                        cb.equal(root.get("applianceManagerConnector"), mc));

        return em.createQuery(query).getResultList();
    }

    public static DistributedAppliance findById(EntityManager em, Long id) {
        return em.find(DistributedAppliance.class, id);
    }

    public static boolean isCompositeKeyExisting(EntityManager em, String mcName, String vcName, long avID) {

        String hql = "SELECT count(*) FROM VirtualSystem VS WHERE VS.cluster.domain.applianceManagerConnector.name = :mcName AND "
                + "VS.virtualizationConnector.name = :vcName AND VS.applianceSoftwareVersion.id = :avID";

        TypedQuery<Long> query = em.createQuery(hql, Long.class);
        query.setParameter("mcName", mcName);
        query.setParameter("vcName", vcName);
        query.setParameter("avID", avID);
        Long count = query.getSingleResult();

        if (count > 0) {

            return true;
        }

        return false;

    }

    public static boolean isReferencedByDistributedAppliance(EntityManager em, ApplianceManagerConnector mc) {
        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<DistributedAppliance> query = cb.createQuery(DistributedAppliance.class);

        Root<DistributedAppliance> root = query.from(DistributedAppliance.class);

        query = query.select(root)
                .where(cb.equal(root.join("applianceManagerConnector").get("id"), mc.getId()));

        return !em.createQuery(query).setMaxResults(1).getResultList().isEmpty();
    }

    public static boolean isReferencedByDistributedAppliance(EntityManager em, ApplianceSoftwareVersion av) {

        String hql = "SELECT count(*) FROM DistributedAppliance DA, VirtualSystem VS WHERE VS.distributedAppliance.id = DA.id AND VS.applianceSoftwareVersion.id = :appSwVerId";
        TypedQuery<Long> query = em.createQuery(hql, Long.class);
        query.setParameter("appSwVerId", av.getId());
        Long count = query.getSingleResult();

        if (count > 0) {

            return true;
        }

        return false;
    }

    public static boolean isProtectingWorkload(DistributedAppliance da) {
        return CollectionUtils.emptyIfNull(da.getVirtualSystems()).stream().anyMatch(vs -> VirtualSystemEntityMgr.isProtectingWorkload(vs));
    }
}
