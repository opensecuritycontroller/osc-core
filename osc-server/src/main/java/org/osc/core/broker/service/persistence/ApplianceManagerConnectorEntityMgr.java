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
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.osc.core.broker.model.entities.appliance.Appliance;
import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.model.plugin.manager.ManagerApiFactory;
import org.osc.core.broker.model.plugin.manager.ManagerType;
import org.osc.core.broker.service.dto.ApplianceManagerConnectorDto;
import org.osc.core.broker.service.dto.job.JobState;
import org.osc.core.broker.service.dto.job.JobStatus;
import org.osc.core.broker.util.db.HibernateUtil;
import org.osc.core.util.EncryptionUtil;
import org.osc.core.util.encryption.EncryptionException;

public class ApplianceManagerConnectorEntityMgr {

    public static ApplianceManagerConnector createEntity(ApplianceManagerConnectorDto dto) throws Exception {
        ApplianceManagerConnector mc = new ApplianceManagerConnector();

        toEntity(mc, dto);

        return mc;
    }

    public static void toEntity(ApplianceManagerConnector mc, ApplianceManagerConnectorDto dto) throws Exception {

        // Transform from dto to entity
        mc.setId(dto.getId());
        mc.setName(dto.getName());
        mc.setManagerType(dto.getManagerType());
        mc.setServiceType(ManagerApiFactory.getServiceName(
                ManagerType.fromText(dto.getManagerType())));
        mc.setIpAddress(dto.getIpAddress());
        mc.setUsername(dto.getUsername());
        mc.setPassword(EncryptionUtil.encryptAESCTR(dto.getPassword()));
        mc.setApiKey(dto.getApiKey());
        mc.setSslCertificateAttrSet(dto.getSslCertificateAttrSet());
    }

    public static void fromEntity(ApplianceManagerConnector mc, ApplianceManagerConnectorDto dto) throws EncryptionException {

        // transform from entity to dto
        dto.setId(mc.getId());
        dto.setName(mc.getName());
        dto.setManagerType(mc.getManagerType());
        dto.setIpAddress(mc.getIpAddress());
        dto.setUsername(mc.getUsername());
        dto.setPassword(EncryptionUtil.decryptAESCTR(mc.getPassword()));
        if (mc.getLastJob() != null) {
            dto.setLastJobStatus(JobStatus.valueOf(mc.getLastJob().getStatus().name()));
            dto.setLastJobState(JobState.valueOf(mc.getLastJob().getState().name()));
            dto.setLastJobId(mc.getLastJob().getId());
        }
        dto.setApiKey(mc.getApiKey());
        dto.setSslCertificateAttrSet(mc.getSslCertificateAttrSet().stream().collect(Collectors.toSet()));
    }

    public static ApplianceManagerConnector findById(EntityManager em, Long id) {

        // Initializing Entity Manager
        OSCEntityManager<ApplianceManagerConnector> emgr = new OSCEntityManager<>(ApplianceManagerConnector.class, em);

        return emgr.findByPrimaryKey(id);
    }

    public static List<ApplianceManagerConnector> listByManagerType(EntityManager em, ManagerType type) {
        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<ApplianceManagerConnector> cq = cb.createQuery(ApplianceManagerConnector.class);
        Root<ApplianceManagerConnector> from = cq.from(ApplianceManagerConnector.class);
        cq = cq.select(from).distinct(true)
                .where(cb.equal(from.get("managerType"), type.getValue()));
        return em.createQuery(cq).getResultList();
    }

    public static boolean isManagerTypeUsed(String managerType) {

        try {
            EntityManager em = HibernateUtil.getTransactionalEntityManager();
            return HibernateUtil.getTransactionControl().required(() -> {
                CriteriaBuilder cb = em.getCriteriaBuilder();
                CriteriaQuery<Long> cq;
                Root<?> from;

                cq = cb.createQuery(Long.class);
                from = cq.from(ApplianceManagerConnector.class);
                cq = cq.select(cb.count(from))
                        .where(cb.equal(from.get("managerType"), managerType));

                Long count1 = em.createQuery(cq).getSingleResult();

                cq = cb.createQuery(Long.class);
                from = cq.from(Appliance.class);
                cq = cq.select(cb.count(from))
                        .where(cb.equal(from.get("managerType"), managerType));

                Long count2 = em.createQuery(cq).getSingleResult();

                return count1 > 0 || count2 > 0;
            });
        } catch (Exception e) {
            return true;
        }

    }

}
