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

import org.osc.core.broker.job.JobState;
import org.osc.core.broker.job.JobStatus;
import org.osc.core.broker.model.entities.appliance.ApplianceSoftwareVersion;
import org.osc.core.broker.model.entities.appliance.DistributedAppliance;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.plugin.sdncontroller.ControllerType;
import org.osc.core.broker.service.dto.VirtualizationConnectorDto;
import org.osc.core.broker.service.dto.VirtualizationType;
import org.osc.core.broker.service.exceptions.VmidcBrokerInvalidRequestException;
import org.osc.core.broker.util.db.HibernateUtil;
import org.osc.core.util.EncryptionUtil;
import org.osc.core.util.encryption.EncryptionException;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class VirtualizationConnectorEntityMgr {

    public static VirtualizationConnector createEntity(VirtualizationConnectorDto dto) throws Exception {
        VirtualizationConnector vc = new VirtualizationConnector();
        toEntity(vc, dto);
        return vc;
    }

    public static void toEntity(VirtualizationConnector vc, VirtualizationConnectorDto dto) throws EncryptionException {

        // transform from dto to entity
        vc.setId(dto.getId());
        vc.setName(dto.getName());
        vc.setVirtualizationType(
                org.osc.core.broker.model.entities.appliance.VirtualizationType.valueOf(
                        dto.getType().name()));

        ControllerType controllerType = dto.getControllerType();
        if(controllerType != null) {
            vc.setControllerType(controllerType.getValue());
        }
        if (dto.isControllerDefined()) {
            vc.setControllerIpAddress(dto.getControllerIP());
            vc.setControllerUsername(dto.getControllerUser());
            vc.setControllerPassword(EncryptionUtil.encryptAESCTR(dto.getControllerPassword()));
        } else {
            vc.setControllerIpAddress(null);
            vc.setControllerUsername(null);
            vc.setControllerPassword(null);
        }

        vc.setProviderIpAddress(dto.getProviderIP());
        vc.setProviderUsername(dto.getProviderUser());
        vc.setProviderPassword(EncryptionUtil.encryptAESCTR(dto.getProviderPassword()));
        vc.setAdminTenantName(dto.getAdminTenantName());
        vc.getProviderAttributes().putAll(dto.getProviderAttributes());
        vc.setSslCertificateAttrSet(dto.getSslCertificateAttrSet());

        vc.setVirtualizationSoftwareVersion(dto.getSoftwareVersion());
    }

    public static void fromEntity(VirtualizationConnector vc, VirtualizationConnectorDto dto) throws EncryptionException {

        // transform from entity to dto
        dto.setId(vc.getId());
        dto.setName(vc.getName());
        dto.setType(VirtualizationType.valueOf(vc.getVirtualizationType().name()));

        dto.setControllerType(ControllerType.fromText(vc.getControllerType()));
        dto.setControllerIP(vc.getControllerIpAddress());
        dto.setControllerUser(vc.getControllerUsername());
        dto.setControllerPassword(EncryptionUtil.decryptAESCTR(vc.getControllerPassword()));

        dto.setProviderIP(vc.getProviderIpAddress());
        dto.setProviderUser(vc.getProviderUsername());
        dto.setProviderPassword(EncryptionUtil.decryptAESCTR(vc.getProviderPassword()));
        dto.setAdminTenantName(vc.getProviderAdminTenantName());
        dto.getProviderAttributes().putAll(vc.getProviderAttributes());
        dto.setSslCertificateAttrSet(vc.getSslCertificateAttrSet().stream().collect(Collectors.toSet()));

        dto.setSoftwareVersion(vc.getVirtualizationSoftwareVersion());

        if (vc.getLastJob() != null) {
            dto.setLastJobStatus(JobStatus.valueOf(vc.getLastJob().getStatus().name()));
            dto.setLastJobState(JobState.valueOf(vc.getLastJob().getState().name()));
            dto.setLastJobId(vc.getLastJob().getId());
        }
    }

    public static VirtualizationConnector findByName(EntityManager em, String name) {

        // Initializing Entity Manager
        OSCEntityManager<VirtualizationConnector> emgr = new OSCEntityManager<VirtualizationConnector>(
                VirtualizationConnector.class, em);

        return emgr.findByFieldName("name", name);
    }

    public static List<VirtualizationConnector> listBySwVersion(EntityManager em, String swVersion) {

        // get appliance software version based on software version provided
        OSCEntityManager<ApplianceSoftwareVersion> emgr1 = new OSCEntityManager<ApplianceSoftwareVersion>(
                ApplianceSoftwareVersion.class, em);
        List<ApplianceSoftwareVersion> asvList = emgr1.listByFieldName("applianceSoftwareVersion", swVersion);

        // Initializing Entity Manager
        OSCEntityManager<VirtualizationConnector> emgr = new OSCEntityManager<VirtualizationConnector>(
                VirtualizationConnector.class, em);

        ArrayList<VirtualizationConnector> vcList = new ArrayList<VirtualizationConnector>();

        // get all VCs based on the appliance software version
        // and virtualization software version.
        for (ApplianceSoftwareVersion avs : asvList) {
            vcList.addAll(
                    emgr.listByFieldName("virtualizationSoftwareVersion", avs.getVirtualizarionSoftwareVersion()));
        }

        return vcList;

    }

    public static void validateCanBeDeleted(EntityManager em, VirtualizationConnector vc)
            throws VmidcBrokerInvalidRequestException {

        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<Long> query = cb.createQuery(Long.class);

        Root<?> root = query.from(DistributedAppliance.class);

        query = query.select(cb.count(root))
            .where(cb.equal(root.join("virtualSystems").get("virtualizationConnector"), vc));

        Long daCount = em.createQuery(query).getSingleResult();

        if (daCount > 0) {
            throw new VmidcBrokerInvalidRequestException(
                    "Cannot delete Virtualization Connector that is referenced by a Distributed Appliance.");
        }

        query = cb.createQuery(Long.class);

        root = query.from(SecurityGroup.class);

        query = query.select(cb.count(root))
            .where(cb.equal(root.get("virtualizationConnector"), vc));

        Long sgCount = em.createQuery(query).getSingleResult();

        if (sgCount > 0) {
            throw new VmidcBrokerInvalidRequestException(
                    "Cannot delete Virtualization Connector that is referenced by Security Groups.");
        }

    }

    public static VirtualizationConnector findById(EntityManager em, Long id) {

        // Initializing Entity Manager
        OSCEntityManager<VirtualizationConnector> emgr = new OSCEntityManager<VirtualizationConnector>(
                VirtualizationConnector.class, em);

        return emgr.findByPrimaryKey(id);
    }

    public static List<VirtualizationConnector> listByType(EntityManager em, VirtualizationType type) {
        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<VirtualizationConnector> query = cb.createQuery(VirtualizationConnector.class);

        Root<VirtualizationConnector> root = query.from(VirtualizationConnector.class);

        query = query.select(root)
            .where(cb.equal(root.get("virtualizationType"), type));

        return em.createQuery(query).getResultList();
    }

    public static boolean isControllerTypeUsed(String controllerType) {

        Long count = 0L;
        try {
            EntityManager em = HibernateUtil.getTransactionalEntityManager();
            count = HibernateUtil.getTransactionControl().required(() -> {
                CriteriaBuilder cb = em.getCriteriaBuilder();

                CriteriaQuery<Long> query = cb.createQuery(Long.class);

                Root<VirtualizationConnector> root = query.from(VirtualizationConnector.class);

                query = query.select(cb.count(root))
                        .where(cb.equal(root.get("controllerType"), controllerType));

                return em.createQuery(query).getSingleResult();
            });
        } catch (Exception e) {
            // Ignore this exception
        }

        return count > 0;
    }

}
