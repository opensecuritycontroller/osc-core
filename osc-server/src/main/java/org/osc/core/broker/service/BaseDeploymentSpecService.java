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
package org.osc.core.broker.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import org.apache.commons.collections4.CollectionUtils;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.virtualization.openstack.AvailabilityZone;
import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.broker.model.entities.virtualization.openstack.Host;
import org.osc.core.broker.model.entities.virtualization.openstack.HostAggregate;
import org.osc.core.broker.rest.client.openstack.openstack4j.Endpoint;
import org.osc.core.broker.rest.client.openstack.openstack4j.Openstack4JNova;
import org.osc.core.broker.service.dto.openstack.AvailabilityZoneDto;
import org.osc.core.broker.service.dto.openstack.DeploymentSpecDto;
import org.osc.core.broker.service.dto.openstack.HostAggregateDto;
import org.osc.core.broker.service.dto.openstack.HostDto;
import org.osc.core.broker.service.exceptions.VmidcBrokerInvalidEntryException;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.HostAggregateEntityMgr;
import org.osc.core.broker.service.persistence.HostEntityMgr;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.persistence.VirtualSystemEntityMgr;
import org.osc.core.broker.service.request.Request;
import org.osc.core.broker.service.response.Response;
import org.osc.core.broker.service.validator.DeploymentSpecDtoValidator;
import org.osc.core.broker.util.ValidateUtil;

public abstract class BaseDeploymentSpecService<I extends Request, O extends Response> extends ServiceDispatcher<I, O> {

    protected VirtualSystem vs;

    protected void validate(EntityManager em, DeploymentSpecDto dto) throws Exception {
        DeploymentSpecDtoValidator.checkForNullFields(dto);
        DeploymentSpecDtoValidator.checkFieldLength(dto);

        this.vs = VirtualSystemEntityMgr.findById(em, dto.getParentId());

        if (this.vs == null || this.vs.getMarkedForDeletion()) {
            throw new VmidcBrokerValidationException(
                    "Deployment Specification using The associated Virtual System with Id: " + dto.getParentId()
                    + "  is either not found or is been marked deleted by the user.");
        }

        if (dto.getCount() == null || dto.getCount() <= 0) {
            throw new VmidcBrokerValidationException("Invalid count " + dto.getCount() == null ? "null"
                    : dto.getCount() + " specified for Deployment Specification");
        }

        validateVirtualizationTypeSpecificNullFields(dto);

        if (this.vs.getVirtualizationConnector().getVirtualizationType().isOpenstack()) {
            if (!dto.getAvailabilityZones().isEmpty() && (!dto.getHosts().isEmpty() || !dto.getHostAggregates().isEmpty())
                    || !dto.getHosts().isEmpty()
                    && (!dto.getAvailabilityZones().isEmpty() || !dto.getHostAggregates().isEmpty())
                    || !dto.getHostAggregates().isEmpty()
                    && (!dto.getHosts().isEmpty() || !dto.getAvailabilityZones().isEmpty())) {
                // If multiple units of deployment are specified, throw an error
                throw new VmidcBrokerValidationException(
                        "Deployment Specification can only be specified with Availablity zones or Hosts or Host Aggregates.");
            }
            if (dto.getHosts().isEmpty() && dto.getCount() != 1) {
                throw new VmidcBrokerValidationException("Invalid count " + dto.getCount()
                + " specified for Deployment Specification. Only valid value is 1.");
            }
            validateSelectionCriteria(this.vs, dto);
        }
    }

	private void validateSelectionCriteria(VirtualSystem vs, DeploymentSpecDto dto) throws Exception {
		try (Openstack4JNova novaApi = new Openstack4JNova(new Endpoint(vs.getVirtualizationConnector()))) {

			// validate availability zones
			if (!dto.getAvailabilityZones().isEmpty()) {
				List<? extends org.openstack4j.model.compute.ext.AvailabilityZone> azList = novaApi
						.listAvailabilityZones(dto.getRegion());
				Set<String> zones = CollectionUtils.isEmpty(azList) ? null
						: azList.stream().map(org.openstack4j.model.compute.ext.AvailabilityZone::getZoneName)
								.collect(Collectors.toSet());
				for (AvailabilityZoneDto az : dto.getAvailabilityZones()) {
					if (!zones.contains(az.getZone())) {
						throw new VmidcBrokerValidationException(String
								.format("Invalid availability zones provided for Deployment Spec '%s'", dto.getName()));
					}
				}
			}

			// validate host aggregates
			if (!dto.getHostAggregates().isEmpty()) {
				List<? extends org.openstack4j.model.compute.HostAggregate> hostAggrList = novaApi
						.listHostAggregates(dto.getRegion());
				Set<String> hostAggr = CollectionUtils.isEmpty(hostAggrList) ? null
						: hostAggrList.stream().map(org.openstack4j.model.compute.HostAggregate::getId)
								.collect(Collectors.toSet());
				for (HostAggregateDto hostAggrDto : dto.getHostAggregates()) {
					if (!hostAggr.contains(hostAggrDto.getOpenstackId())) {
						throw new VmidcBrokerValidationException(String
								.format("Invalid host aggregates provided for Deployment Spec '%s'", dto.getName()));
					}
				}
			}

			// validate hosts
			if (!dto.getHosts().isEmpty()) {
				Set<String> hosts = novaApi.getComputeHosts(dto.getRegion());
				for (HostDto host : dto.getHosts()) {
					if (!hosts.contains(host.getName())) {
						throw new VmidcBrokerValidationException(
								String.format("Invalid hosts provided for Deployment Spec '%s'", dto.getName()));
					}
				}
			}
		}
	}

	protected void throwInvalidUpdateActionException(String attributeType, String dsName)
            throws VmidcBrokerValidationException {
        throw new VmidcBrokerValidationException(String.format(
                "'%s' attribute cannot be updated for Deployment Spec '%s'", attributeType, dsName));
    }

    protected HostAggregate createHostAggregate(EntityManager em, HostAggregateDto haDto, DeploymentSpec ds) {
        HostAggregate ha = new HostAggregate(ds, haDto.getOpenstackId(), haDto.getName());
        HostAggregateEntityMgr.toEntity(ha, haDto);
        return OSCEntityManager.create(em, ha, this.txBroadcastUtil);
    }

    protected AvailabilityZone createAvailabilityZone(EntityManager em, AvailabilityZoneDto azDto, DeploymentSpec ds) {
        AvailabilityZone az = new AvailabilityZone(ds, azDto.getRegion(), azDto.getZone());
        return OSCEntityManager.create(em, az, this.txBroadcastUtil);
    }

    protected Host createHost(EntityManager em, HostDto hostDto, DeploymentSpec ds) {
        Host hs = new Host(ds, hostDto.getOpenstackId());
        HostEntityMgr.toEntity(hs, hostDto);
        return OSCEntityManager.create(em, hs, this.txBroadcastUtil);
    }

    private void validateVirtualizationTypeSpecificNullFields(DeploymentSpecDto dto) throws VmidcBrokerInvalidEntryException {
        Map<String, Object> map = new HashMap<>();

        if (this.vs.getVirtualizationConnector().getVirtualizationType().isOpenstack()) {
            map.put("Project Name", dto.getProjectName());
            map.put("Project", dto.getProjectId());

            map.put("Region", dto.getRegion());

            map.put("Virtual System Id", dto.getParentId());

            map.put("Management Network Name", dto.getManagementNetworkName());
            map.put("Management Network Id", dto.getManagementNetworkId());

            map.put("Inspection Network Name", dto.getInspectionNetworkName());
            map.put("Inspection Network Id", dto.getInspectionNetworkId());

            map.put("Instance Count", dto.getCount());
        } else if (this.vs.getVirtualizationConnector().getVirtualizationType().isKubernetes()) {
            map.put("Namespace", dto.getNamespace());
        }

        ValidateUtil.checkForNullFields(map);
    }
}
