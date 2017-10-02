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
import javax.persistence.criteria.Root;

import org.apache.commons.collections4.CollectionUtils;
import org.osc.core.broker.model.entities.appliance.DistributedAppliance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.virtualization.openstack.AvailabilityZone;
import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.broker.model.entities.virtualization.openstack.HostAggregate;
import org.osc.core.broker.service.dto.openstack.AvailabilityZoneDto;
import org.osc.core.broker.service.dto.openstack.DeploymentSpecDto;
import org.osc.core.broker.service.dto.openstack.HostAggregateDto;

public class DeploymentSpecEntityMgr {

    public static DeploymentSpec createEntity(// for add
            DeploymentSpecDto dto, VirtualSystem vs) throws Exception {
        DeploymentSpec ds = new DeploymentSpec(vs, dto.getRegion(), dto.getProjectId(), dto.getManagementNetworkId(),
                dto.getInspectionNetworkId(), dto.getFloatingIpPoolName());
        toEntity(ds, dto);
        return ds;

    }

    public static void toEntity(DeploymentSpec ds, DeploymentSpecDto dto) {
        ds.setName(dto.getName());
        ds.setManagementNetworkName(dto.getManagementNetworkName());
        ds.setInspectionNetworkName(dto.getInspectionNetworkName());
        ds.setProjectName(dto.getProjectName());
        ds.setInstanceCount(dto.getCount());
        ds.setShared(dto.isShared());
        ds.setNamespace(dto.getNamespace());
    }

    public static void fromEntity(DeploymentSpec ds, DeploymentSpecDto dto) {
        dto.setId(ds.getId());
        dto.setParentId(ds.getVirtualSystem().getId());
        dto.setName(ds.getName());
        dto.setProjectName(ds.getProjectName());
        dto.setProjectId(ds.getProjectId());
        dto.setRegion(ds.getRegion());
        dto.setFloatingIpPoolName(ds.getFloatingIpPoolName());
        dto.setManagementNetworkName(ds.getManagementNetworkName());
        dto.setManagementNetworkId(ds.getManagementNetworkId());
        dto.setInspectionNetworkName(ds.getInspectionNetworkName());
        dto.setInspectionNetworkId(ds.getInspectionNetworkId());
        dto.setPortGroupId(ds.getPortGroupId());
        dto.setCount(ds.getInstanceCount());
        dto.setNamespace(ds.getNamespace());
        dto.setExternalId(ds.getExternalId());
        if (ds.getLastJob() != null) {
            dto.setLastJobStatus(ds.getLastJob().getStatus().name());
            dto.setLastJobState(ds.getLastJob().getState().name());
            dto.setLastJobId(ds.getLastJob().getId());
        }
        dto.setShared(ds.isShared());
        dto.setMarkForDeletion(ds.getMarkedForDeletion());
        if (!ds.getAvailabilityZones().isEmpty()) {
            Set<AvailabilityZoneDto> azDtoSet = new HashSet<>();
            for (AvailabilityZone az : ds.getAvailabilityZones()) {
                AvailabilityZoneDto azDto = new AvailabilityZoneDto();
                AvailabilityZoneEntityMgr.fromEntity(az, azDto);
                azDtoSet.add(azDto);
            }
            dto.setAvailabilityZones(azDtoSet);
        } else if (!ds.getHosts().isEmpty()) {
            dto.setHosts(HostEntityMgr.fromEntity(ds.getHosts()));
        } else if (!ds.getHostAggregates().isEmpty()) {
            Set<HostAggregateDto> hostAggrSet = new HashSet<>();
            for (HostAggregate hostAggr : ds.getHostAggregates()) {
                HostAggregateDto hostAggrDto = new HostAggregateDto();
                HostAggregateEntityMgr.fromEntity(hostAggr, hostAggrDto);
                hostAggrSet.add(hostAggrDto);
            }
            dto.setHostAggregates(hostAggrSet);
        }
    }

    public static List<DeploymentSpec> listDeploymentSpecByVirtualSystem(EntityManager em, VirtualSystem vs) {
        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<DeploymentSpec> query = cb.createQuery(DeploymentSpec.class);

        Root<DeploymentSpec> root = query.from(DeploymentSpec.class);

        query = query.select(root).distinct(true)
                .where(cb.equal(root.get("virtualSystem"), vs));

        query = query.orderBy(cb.asc(root.get("name")));
        return em.createQuery(query).getResultList();
    }

    public static DeploymentSpec findDeploymentSpecByVirtualSystemProjectAndRegion(EntityManager em, VirtualSystem vs,
            String projectId, String region) {

        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<DeploymentSpec> query = cb.createQuery(DeploymentSpec.class);

        Root<DeploymentSpec> root = query.from(DeploymentSpec.class);

        query = query.select(root)
                .where(cb.equal(root.get("projectId"), projectId),
                        cb.equal(root.get("region"), region),
                        cb.equal(root.get("virtualSystem"), vs));

        try {
            return em.createQuery(query).getSingleResult();
        } catch (NoResultException nre) {
            return null;
        }
    }

    // TODO Larkins: Remove the hard coded region
    public static List<DeploymentSpec> findDeploymentSpecsByVirtualSystemProjectWithDefaultRegionOne(EntityManager em,
            VirtualSystem vs, String projectId) {

        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<DeploymentSpec> query = cb.createQuery(DeploymentSpec.class);

        Root<DeploymentSpec> root = query.from(DeploymentSpec.class);

        query = query.select(root).distinct(true)
                .where(cb.equal(root.get("projectId"), projectId),
                        cb.equal(root.get("virtualSystem"), vs),
                        cb.equal(root.get("region"), "RegionOne"));

        return em.createQuery(query).getResultList();
    }

    public static List<DeploymentSpec> findDeploymentSpecsByVirtualSystemProjectAndRegion(EntityManager em,
            VirtualSystem vs, String projectId, String region) {

        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<DeploymentSpec> query = cb.createQuery(DeploymentSpec.class);

        Root<DeploymentSpec> root = query.from(DeploymentSpec.class);

        query = query.select(root).distinct(true)
                .where(cb.equal(root.get("projectId"), projectId),
                        cb.equal(root.get("region"), region),
                        cb.equal(root.get("virtualSystem"), vs));

        return em.createQuery(query).getResultList();
    }

    public static DeploymentSpec findById(EntityManager em, Long id) {
        return em.find(DeploymentSpec.class, id);
    }

    public static List<DeploymentSpec> listDeploymentSpecByProjectId(EntityManager em, String projectId) {
        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<DeploymentSpec> query = cb.createQuery(DeploymentSpec.class);

        Root<DeploymentSpec> root = query.from(DeploymentSpec.class);

        query = query.select(root).distinct(true)
                .where(cb.equal(root.get("projectId"), projectId));

        return em.createQuery(query).getResultList();
    }

    public static boolean isDeploymentSpecAllHostInRegion(DeploymentSpec ds) {
        return ds.getAvailabilityZones().isEmpty() && ds.getHostAggregates().isEmpty() && ds.getHosts().isEmpty();
    }

    public static List<DeploymentSpec> listDeploymentSpecByDistributedAppliance(EntityManager em, DistributedAppliance da) {
        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<DeploymentSpec> query = cb.createQuery(DeploymentSpec.class);

        Root<DeploymentSpec> root = query.from(DeploymentSpec.class);

        query = query.select(root).distinct(true)
                .where(root.get("virtualSystem").in(da.getVirtualSystems()));

        return em.createQuery(query).getResultList();
    }

    public static boolean isProtectingWorkload(DeploymentSpec ds) {
        return CollectionUtils.emptyIfNull(ds.getDistributedApplianceInstances()).stream().anyMatch(dai -> !dai.getProtectedPorts().isEmpty());
    }
}
