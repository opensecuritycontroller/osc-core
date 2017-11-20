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


import static java.util.stream.Collectors.toSet;
import static org.osc.core.common.virtualization.VirtualizationConnectorProperties.ATTRIBUTE_KEY_RABBITMQ_USER_PASSWORD;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.osc.core.broker.model.entities.appliance.ApplianceSoftwareVersion;
import org.osc.core.broker.model.entities.appliance.DistributedAppliance;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.service.api.server.EncryptionApi;
import org.osc.core.broker.service.api.server.EncryptionException;
import org.osc.core.broker.service.dto.VirtualizationConnectorDto;
import org.osc.core.broker.service.exceptions.VmidcBrokerInvalidRequestException;
import org.osc.core.broker.util.TransactionalBroadcastUtil;
import org.osc.core.common.virtualization.VirtualizationType;

public class VirtualizationConnectorEntityMgr {

    public static VirtualizationConnector createEntity(VirtualizationConnectorDto dto,
            EncryptionApi encryption) throws Exception {
        VirtualizationConnector vc = new VirtualizationConnector();
        toEntity(vc, dto, encryption);
        return vc;
    }

    public static void toEntity(VirtualizationConnector vc, VirtualizationConnectorDto dto,
            EncryptionApi encryption) throws EncryptionException {

        // transform from dto to entity
        vc.setId(dto.getId());
        vc.setName(dto.getName());
        vc.setVirtualizationType(dto.getType());

        String controllerType = dto.getControllerType() != null ? dto.getControllerType() : null;
        if(controllerType != null) {
            vc.setControllerType(controllerType);
        }
        if (dto.isControllerDefined()) {
            vc.setControllerIpAddress(dto.getControllerIP());
            vc.setControllerUsername(dto.getControllerUser());
            vc.setControllerPassword(encryption.encryptAESCTR(dto.getControllerPassword()));
        } else {
            vc.setControllerIpAddress(null);
            vc.setControllerUsername(null);
            vc.setControllerPassword(null);
        }

        vc.setProviderIpAddress(dto.getProviderIP());
        vc.setProviderUsername(dto.getProviderUser());
        vc.setProviderPassword(encryption.encryptAESCTR(dto.getProviderPassword()));
        vc.setAdminProjectName(dto.getAdminProjectName());
        vc.setAdminDomainId(dto.getAdminDomainId());
        vc.getProviderAttributes().putAll(dto.getProviderAttributes());

        // For rabbit MQ password, encrypt it before setting it on the entity.
        String rabbitMqPassword = vc.getProviderAttributes().get(ATTRIBUTE_KEY_RABBITMQ_USER_PASSWORD);
        vc.getProviderAttributes().put(ATTRIBUTE_KEY_RABBITMQ_USER_PASSWORD,
                encryption.encryptAESCTR(rabbitMqPassword));

        vc.setSslCertificateAttrSet(dto.getSslCertificateAttrSet()
                .stream()
                .map(SslCertificateAttrEntityMgr::createEntity)
                .collect(toSet()));

        vc.setVirtualizationSoftwareVersion(dto.getSoftwareVersion());
    }

    public static void fromEntity(VirtualizationConnector vc, VirtualizationConnectorDto dto,
            EncryptionApi encryption) throws EncryptionException {

        // transform from entity to dto
        dto.setId(vc.getId());
        dto.setName(vc.getName());
        dto.setType(VirtualizationType.valueOf(vc.getVirtualizationType().name()));

        dto.setControllerType(vc.getControllerType());
        dto.setControllerIP(vc.getControllerIpAddress());
        dto.setControllerUser(vc.getControllerUsername());
        dto.setControllerPassword(encryption.decryptAESCTR(vc.getControllerPassword()));

        dto.setProviderIP(vc.getProviderIpAddress());
        dto.setProviderUser(vc.getProviderUsername());
        dto.setProviderPassword(encryption.decryptAESCTR(vc.getProviderPassword()));
        dto.setAdminProjectName(vc.getProviderAdminProjectName());
        dto.setAdminDomainId(vc.getAdminDomainId());
        dto.getProviderAttributes().putAll(vc.getProviderAttributes());

        // For rabbit MQ password, decrypt it before setting it on the dto.
        String rabbitMqPassword = dto.getProviderAttributes().get(ATTRIBUTE_KEY_RABBITMQ_USER_PASSWORD);
        dto.getProviderAttributes().put(ATTRIBUTE_KEY_RABBITMQ_USER_PASSWORD,
                encryption.decryptAESCTR(rabbitMqPassword));

        dto.setSslCertificateAttrSet(vc.getSslCertificateAttrSet().stream()
                .map(SslCertificateAttrEntityMgr::fromEntity)
                .collect(toSet()));

        dto.setSoftwareVersion(vc.getVirtualizationSoftwareVersion());
    }

    public static VirtualizationConnector findByName(EntityManager em, String name,
            TransactionalBroadcastUtil txBroadcastUtil) {

        // Initializing Entity Manager
        OSCEntityManager<VirtualizationConnector> emgr = new OSCEntityManager<>(
                VirtualizationConnector.class, em, txBroadcastUtil);

        return emgr.findByFieldName("name", name);
    }

    public static List<VirtualizationConnector> listBySwVersion(EntityManager em, String swVersion,
            TransactionalBroadcastUtil txBroadcastUtil) {

        // get appliance software version based on software version provided
        OSCEntityManager<ApplianceSoftwareVersion> emgr1 = new OSCEntityManager<>(
                ApplianceSoftwareVersion.class, em, txBroadcastUtil);
        List<ApplianceSoftwareVersion> asvList = emgr1.listByFieldName("applianceSoftwareVersion", swVersion);

        // Initializing Entity Manager
        OSCEntityManager<VirtualizationConnector> emgr = new OSCEntityManager<>(
                VirtualizationConnector.class, em, txBroadcastUtil);

        ArrayList<VirtualizationConnector> vcList = new ArrayList<>();

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

        return em.find(VirtualizationConnector.class, id);
    }

    public static List<VirtualizationConnector> listByType(EntityManager em, VirtualizationType type) {
        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<VirtualizationConnector> query = cb.createQuery(VirtualizationConnector.class);

        Root<VirtualizationConnector> root = query.from(VirtualizationConnector.class);

        query = query.select(root)
                .where(cb.equal(root.get("virtualizationType"), type));

        return em.createQuery(query).getResultList();
    }

    public static boolean isControllerTypeUsed(String controllerType, EntityManager em) {

        Long count = 0L;
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();

            CriteriaQuery<Long> query = cb.createQuery(Long.class);

            Root<VirtualizationConnector> root = query.from(VirtualizationConnector.class);

            query = query.select(cb.count(root))
                    .where(cb.equal(root.get("controllerType"), controllerType));

            count = em.createQuery(query).getSingleResult();
        } catch (Exception e) {
            // Ignore this exception
        }

        return count > 0;
    }

}
