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

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.osc.core.broker.model.entities.appliance.DistributedAppliance;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.broker.model.entities.virtualization.openstack.VMPort;
import org.osc.core.broker.service.dto.DistributedApplianceInstanceDto;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.util.TransactionalBroadcastUtil;

public class DistributedApplianceInstanceEntityMgr {

    public static DistributedApplianceInstanceDto fromEntity(DistributedApplianceInstance dai, Boolean providesDeviceStatus) throws Exception {
        String discovered = providesDeviceStatus ? (dai.getDiscovered() != null ? dai.getDiscovered().toString() : "") : "N/A";
        String inspectionReady = providesDeviceStatus ? (dai.getInspectionReady() != null ? dai.getInspectionReady().toString() : "") : "N/A";
        String lastStatus = providesDeviceStatus ? (dai.getLastStatus() != null ? dai.getLastStatus().toString() : "") : "N/A";

        DistributedApplianceInstanceDto dto = new DistributedApplianceInstanceDto(
                providesDeviceStatus, discovered, inspectionReady, lastStatus);
        dto.setId(dai.getId());

        dto.setVirtualsystemId(dai.getVirtualSystem().getId());
        dto.setVcId(dai.getVirtualSystem().getVirtualizationConnector().getId());
        dto.setMcId(dai.getVirtualSystem().getDistributedAppliance().getApplianceManagerConnector().getId());
        dto.setName(dai.getName());
        dto.setIpAddress(dai.getIpAddress());

        dto.setApplianceModel(dai.getVirtualSystem().getDistributedAppliance().getAppliance().getModel());
        dto.setSwVersion(dai.getVirtualSystem().getDistributedAppliance().getApplianceVersion());

        dto.setDistributedApplianceName(dai.getVirtualSystem().getDistributedAppliance().getName());
        dto.setApplianceManagerConnectorName(dai.getVirtualSystem().getDistributedAppliance()
                .getApplianceManagerConnector().getName());
        dto.setVirtualConnectorName(dai.getVirtualSystem().getVirtualizationConnector().getName());
        dto.setHostname(dai.getHostName());

        dto.setOsVmId(dai.getOsServerId());
        dto.setOsHostname(dai.getOsHostName());
        dto.setOsInspectionIngressPortId(dai.getInspectionOsIngressPortId());
        dto.setOsInspectionIngressMacAddress(dai.getInspectionIngressMacAddress());
        dto.setOsInspectionEgressPortId(dai.getInspectionOsEgressPortId());
        dto.setOsInspectionEgressMacAddress(dai.getInspectionEgressMacAddress());

        dto.setMgmtIpAddress(dai.getMgmtIpAddress());
        dto.setMgmtSubnetPrefixLength(dai.getMgmtSubnetPrefixLength());
        dto.setMgmtGateway(dai.getMgmtGateway());

        return dto;
    }

    public static boolean doesDAIExist(EntityManager em) {

        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<DistributedApplianceInstance> query = cb.createQuery(DistributedApplianceInstance.class);
        query = query.select(query.from(DistributedApplianceInstance.class));

        List<DistributedApplianceInstance> list = em.createQuery(query).setMaxResults(1).getResultList();

        if (list == null || list.size() == 0) {
            return false;
        }

        return true;
    }

    public static DistributedApplianceInstance findByNsxAgentIdAndNsxIp(EntityManager em, String nsxAgentId,
            String nsxIpAddress) {
        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<DistributedApplianceInstance> query = cb.createQuery(DistributedApplianceInstance.class);

        Root<DistributedApplianceInstance> from = query.from(DistributedApplianceInstance.class);

        query = query.select(from).where(
                cb.equal(from.get("nsxAgentId"), nsxAgentId),
                cb.equal(from.join("virtualSystem").join("virtualizationConnector").get("controllerIpAddress"), nsxIpAddress));

        try {
            return em.createQuery(query).getSingleResult();
        } catch (NoResultException nre) {
            return null;
        }
    }

    public static DistributedApplianceInstance findByOsHostNameAndOsTenantId(EntityManager em, String osHostName,
            String osTenantId) {

        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<DistributedApplianceInstance> query = cb.createQuery(DistributedApplianceInstance.class);

        Root<DistributedApplianceInstance> from = query.from(DistributedApplianceInstance.class);

        query = query.select(from).where(
                cb.equal(from.get("osHostName"), osHostName),
                cb.equal(from.get("osTenantId"), osTenantId));

        try {
            return em.createQuery(query).getSingleResult();
        } catch (NoResultException nre) {
            return null;
        }
    }

    public static List<DistributedApplianceInstance> listByVsId(EntityManager em, Long vsId) {
        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<DistributedApplianceInstance> query = cb.createQuery(DistributedApplianceInstance.class);

        Root<DistributedApplianceInstance> from = query.from(DistributedApplianceInstance.class);

        query = query.select(from).distinct(true).where(
                cb.equal(from.join("virtualSystem").get("id"), vsId));

        return em.createQuery(query).getResultList();
    }

    /**
     * Get DAI's by Id. If unable to find any DAI, throws VmidcBrokerValidationException.
     * @param em
     * @param daiIds
     * @return
     * @throws VmidcBrokerValidationException
     */
    public static List<DistributedApplianceInstance> getByIds(EntityManager em, List<Long> daiIds)
            throws VmidcBrokerValidationException {
        List<DistributedApplianceInstance> daiList = new ArrayList<>();
        if (daiIds != null) {
            for (Long daiId : daiIds) {
                // fetching DAIs based upon received DAI-DTOs
                DistributedApplianceInstance dai = findById(em, daiId);
                if (dai == null) {
                    throw new VmidcBrokerValidationException(
                            "Distributed Appliance Instance with ID " + daiId + " is not found.");
                }
                daiList.add(dai);
            }
        }

        return daiList;
    }

    public static List<Long> listByMcId(EntityManager em, Long mcId) {

        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<Long> query = cb.createQuery(Long.class);

        Root<DistributedApplianceInstance> from = query.from(DistributedApplianceInstance.class);

        query = query.select(from.get("id")).distinct(true).where(
                cb.equal(from.join("virtualSystem").join("distributedAppliance")
                        .join("applianceManagerConnector").get("id"), mcId));

        List<Long> list = em.createQuery(query).getResultList();

        if (list == null || list.size() == 0) {
            return null;
        }

        return list;
    }

    public static List<Long> listByVcId(EntityManager em, Long vcId) {

        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<Long> query = cb.createQuery(Long.class);

        Root<DistributedApplianceInstance> from = query.from(DistributedApplianceInstance.class);

        query = query.select(from.get("id")).distinct(true).where(
                cb.equal(from.join("virtualSystem").join("virtualizationConnector")
                        .get("id"), vcId));

        List<Long> list = em.createQuery(query).getResultList();

        if (list == null || list.size() == 0) {
            return null;
        }

        return list;
    }

    public static List<String> listOsServerIdByVcId(EntityManager em, Long vcId) {

        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<String> query = cb.createQuery(String.class);

        Root<DistributedApplianceInstance> from = query.from(DistributedApplianceInstance.class);

        query = query.select(from.get("osServerId")).distinct(true).where(
                cb.equal(from.join("virtualSystem").join("virtualizationConnector")
                        .get("id"), vcId));

        return em.createQuery(query).getResultList();
    }

    public static List<Long> listByDaId(EntityManager em, Long daId) {

        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<Long> query = cb.createQuery(Long.class);

        Root<DistributedApplianceInstance> from = query.from(DistributedApplianceInstance.class);

        query = query.select(from.get("id")).distinct(true).where(
                cb.equal(from.join("virtualSystem").join("distributedAppliance")
                        .get("id"), daId));

        List<Long> list = em.createQuery(query).getResultList();

        if (list == null || list.size() == 0) {
            return null;
        }

        return list;
    }

    public static DistributedApplianceInstance findById(EntityManager em, Long id) {
        return em.find(DistributedApplianceInstance.class, id);
    }

    public static DistributedApplianceInstance findByName(EntityManager em, String name,
            TransactionalBroadcastUtil txBroadcastUtil) {

        OSCEntityManager<DistributedApplianceInstance> emgr = new OSCEntityManager<DistributedApplianceInstance>(
                DistributedApplianceInstance.class, em, txBroadcastUtil);

        return emgr.findByFieldName("name", name);
    }

    public static DistributedApplianceInstance findByIpAddress(EntityManager em, String ipAddress) {
        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<DistributedApplianceInstance> query = cb.createQuery(DistributedApplianceInstance.class);

        Root<DistributedApplianceInstance> root = query.from(DistributedApplianceInstance.class);
        query = query.select(root).where(
                cb.equal(root.get("ipAddress"), ipAddress));

        try {
            return em.createQuery(query).getSingleResult();
        } catch (NoResultException nre) {
            return null;
        }
    }

    public static List<DistributedApplianceInstance> listByDsIdAndAvailabilityZone(EntityManager em, Long dsId,
            String availabilityZone) {

        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<DistributedApplianceInstance> query = cb.createQuery(DistributedApplianceInstance.class);

        Root<DistributedApplianceInstance> root = query.from(DistributedApplianceInstance.class);

        query = query.select(root).distinct(true)
                .where(cb.equal(root.join("deploymentSpec").get("id"), dsId),
                       cb.equal(root.get("osAvailabilityZone"), availabilityZone));

        List<DistributedApplianceInstance> list = em.createQuery(query).getResultList();

        if (list == null || list.size() == 0) {
            return null;
        }

        return list;
    }

    public static List<DistributedApplianceInstance> listByDsAndHostName(EntityManager em, DeploymentSpec ds,
            String hostName) {

        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<DistributedApplianceInstance> query = cb.createQuery(DistributedApplianceInstance.class);

        Root<DistributedApplianceInstance> root = query.from(DistributedApplianceInstance.class);

        query = query.select(root).distinct(true)
                .where(cb.equal(root.get("deploymentSpec"), ds),
                       cb.equal(root.get("osHostName"), hostName));

        List<DistributedApplianceInstance> list = em.createQuery(query).getResultList();

        if (list == null || list.size() == 0) {
            return null;
        }

        return list;
    }

    public static boolean isReferencedByDistributedApplianceInstance(EntityManager em, DistributedAppliance da) {

        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<DistributedApplianceInstance> query = cb.createQuery(DistributedApplianceInstance.class);

        Root<DistributedApplianceInstance> root = query.from(DistributedApplianceInstance.class);

        query = query.select(root)
                .where(cb.equal(root.join("virtualSystem").join("distributedAppliance")
                        .get("id"), da.getId()));

        return !em.createQuery(query).setMaxResults(1).getResultList().isEmpty();
    }

    public static DistributedApplianceInstance getByOSServerId(EntityManager em, String osServerId) {

        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<DistributedApplianceInstance> query = cb.createQuery(DistributedApplianceInstance.class);

        Root<DistributedApplianceInstance> root = query.from(DistributedApplianceInstance.class);

        query = query.select(root)
                .where(cb.equal(root.get("osServerId"), osServerId));

        List<DistributedApplianceInstance> list = em.createQuery(query).getResultList();

        if (list == null || list.size() == 0) {
            return null;
        }

        return list.get(0);
    }

    public static DistributedApplianceInstance findByVirtualSystemAndPort(EntityManager em, VirtualSystem vs, VMPort port) {

        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<DistributedApplianceInstance> query = cb.createQuery(DistributedApplianceInstance.class);

        Root<DistributedApplianceInstance> root = query.from(DistributedApplianceInstance.class);

        query = query.select(root)
                .where(cb.equal(root.join("virtualSystem").get("id"), vs.getId()),
                       cb.equal(root.join("protectedPorts").get("id"), port.getId()));

        try {
            return em.createQuery(query).getSingleResult();
        } catch (NoResultException nre) {
            return null;
        }
    }

}
