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
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.osc.core.broker.job.JobState;
import org.osc.core.broker.job.JobStatus;
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
        DeploymentSpec ds = new DeploymentSpec(vs, dto.getRegion(), dto.getTenantId(), dto.getManagementNetworkId(),
                dto.getInspectionNetworkId(), dto.getFloatingIpPoolName());
        toEntity(ds, dto);
        return ds;

    }

    public static void toEntity(DeploymentSpec ds, DeploymentSpecDto dto) {
        ds.setName(dto.getName());
        ds.setManagementNetworkName(dto.getManagementNetworkName());
        ds.setInspectionNetworkName(dto.getInspectionNetworkName());
        ds.setTenantName(dto.getTenantName());
        ds.setInstanceCount(dto.getCount());
        ds.setShared(dto.isShared());
    }

    public static void fromEntity(DeploymentSpec ds, DeploymentSpecDto dto) {
        dto.setId(ds.getId());
        dto.setParentId(ds.getVirtualSystem().getId());
        dto.setName(ds.getName());
        dto.setTenantName(ds.getTenantName());
        dto.setTenantId(ds.getTenantId());
        dto.setRegion(ds.getRegion());
        dto.setFloatingIpPoolName(ds.getFloatingIpPoolName());
        dto.setManagementNetworkName(ds.getManagementNetworkName());
        dto.setManagementNetworkId(ds.getManagementNetworkId());
        dto.setInspectionNetworkName(ds.getInspectionNetworkName());
        dto.setInspectionNetworkId(ds.getInspectionNetworkId());
        dto.setCount(ds.getInstanceCount());
        if (ds.getLastJob() != null) {
            dto.setLastJobStatus(JobStatus.valueOf(ds.getLastJob().getStatus().name()));
            dto.setLastJobState(JobState.valueOf(ds.getLastJob().getState().name()));
            dto.setLastJobId(ds.getLastJob().getId());
        }
        dto.setShared(ds.isShared());
        dto.setMarkForDeletion(ds.getMarkedForDeletion());
        if (!ds.getAvailabilityZones().isEmpty()) {
            Set<AvailabilityZoneDto> azDtoSet = new HashSet<AvailabilityZoneDto>();
            for (AvailabilityZone az : ds.getAvailabilityZones()) {
                AvailabilityZoneDto azDto = new AvailabilityZoneDto();
                AvailabilityZoneEntityMgr.fromEntity(az, azDto);
                azDtoSet.add(azDto);
            }
            dto.setAvailabilityZones(azDtoSet);
        } else if (!ds.getHosts().isEmpty()) {
            dto.setHosts(HostEntityMgr.fromEntity(ds.getHosts()));
        } else if (!ds.getHostAggregates().isEmpty()) {
            Set<HostAggregateDto> hostAggrSet = new HashSet<HostAggregateDto>();
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

    public static DeploymentSpec findDeploymentSpecByVirtualSystemTenantAndRegion(EntityManager em, VirtualSystem vs,
            String tenantId, String region) {

        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<DeploymentSpec> query = cb.createQuery(DeploymentSpec.class);

        Root<DeploymentSpec> root = query.from(DeploymentSpec.class);

        query = query.select(root)
                .where(cb.equal(root.get("tenantId"), tenantId),
                       cb.equal(root.get("region"), region),
                       cb.equal(root.get("virtualSystem"), vs));

        List<DeploymentSpec> list = em.createQuery(query).setMaxResults(1).getResultList();
        return list.isEmpty() ? null : list.get(0);
    }

	public static List<DeploymentSpec> findDeploymentSpecsByVirtualSystemTenantAndRegion(EntityManager em,
			VirtualSystem vs, String tenantId, String region) {

        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<DeploymentSpec> query = cb.createQuery(DeploymentSpec.class);

        Root<DeploymentSpec> root = query.from(DeploymentSpec.class);

        query = query.select(root).distinct(true)
                .where(cb.equal(root.get("tenantId"), tenantId),
                       cb.equal(root.get("region"), region),
                       cb.equal(root.get("virtualSystem"), vs));

        return em.createQuery(query).getResultList();
	}

    public static DeploymentSpec findById(EntityManager em, Long id) {

        // Initializing Entity Manager
        OSCEntityManager<DeploymentSpec> emgr = new OSCEntityManager<DeploymentSpec>(DeploymentSpec.class, em);

        return emgr.findByPrimaryKey(id);
    }

    public static List<DeploymentSpec> listDeploymentSpecByTenentId(EntityManager em, String tenantId) {
        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<DeploymentSpec> query = cb.createQuery(DeploymentSpec.class);

        Root<DeploymentSpec> root = query.from(DeploymentSpec.class);

        query = query.select(root).distinct(true)
                .where(cb.equal(root.get("tenantId"), tenantId));

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
}
