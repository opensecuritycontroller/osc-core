/*******************************************************************************
 * Copyright (c) 2017 Intel Corporation
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

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.osc.core.broker.model.entities.appliance.DistributedAppliance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.virtualization.openstack.AvailabilityZone;
import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.broker.model.entities.virtualization.openstack.HostAggregate;
import org.osc.core.broker.service.dto.openstack.AvailabilityZoneDto;
import org.osc.core.broker.service.dto.openstack.DeploymentSpecDto;
import org.osc.core.broker.service.dto.openstack.HostAggregateDto;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
            dto.setLastJobStatus(ds.getLastJob().getStatus());
            dto.setLastJobState(ds.getLastJob().getState());
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

    @SuppressWarnings("unchecked")
    public static List<DeploymentSpec> listDeploymentSpecByVirtualSystem(Session session, VirtualSystem vs,
            Order[] orders) {
        Criteria criteria = session.createCriteria(DeploymentSpec.class).add(Restrictions.eq("virtualSystem", vs))
                .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
        if (orders != null) {
            for (Order order : orders) {
                criteria.addOrder(order);
            }
        }
        return criteria.list();
    }

    public static DeploymentSpec findDeploymentSpecByVirtualSystemTenantAndRegion(Session session, VirtualSystem vs,
            String tenantId, String region) {

        Criteria criteria = session.createCriteria(DeploymentSpec.class).add(Restrictions.eq("tenantId", tenantId))
                .add(Restrictions.eq("region", region)).add(Restrictions.eq("virtualSystem", vs));
        return (DeploymentSpec) criteria.uniqueResult();
    }

	@SuppressWarnings("unchecked")
	public static List<DeploymentSpec> findDeploymentSpecsByVirtualSystemTenantAndRegion(Session session,
			VirtualSystem vs, String tenantId, String region) {

		Criteria criteria = session.createCriteria(DeploymentSpec.class).add(Restrictions.eq("tenantId", tenantId))
				.add(Restrictions.eq("region", region)).add(Restrictions.eq("virtualSystem", vs))
				.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
		return criteria.list();
	}

    public static DeploymentSpec findById(Session session, Long id) {

        // Initializing Entity Manager
        EntityManager<DeploymentSpec> emgr = new EntityManager<DeploymentSpec>(DeploymentSpec.class, session);

        return emgr.findByPrimaryKey(id);
    }

    @SuppressWarnings("unchecked")
    public static List<DeploymentSpec> listDeploymentSpecByTenentId(Session session, String tenantId) {
        Criteria criteria = session.createCriteria(DeploymentSpec.class).add(Restrictions.eq("tenantId", tenantId))
                .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
        return criteria.list();
    }

    public static boolean isDeploymentSpecAllHostInRegion(DeploymentSpec ds) {
        return ds.getAvailabilityZones().isEmpty() && ds.getHostAggregates().isEmpty() && ds.getHosts().isEmpty();
    }

    @SuppressWarnings("unchecked")
    public static List<DeploymentSpec> listDeploymentSpecByDistributedAppliance(Session session, DistributedAppliance da) {
        Criteria criteria = session.createCriteria(DeploymentSpec.class).add(Restrictions.in("virtualSystem", da.getVirtualSystems()))
                .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);

        return criteria.list();
    }
}
