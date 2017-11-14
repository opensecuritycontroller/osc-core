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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.persistence.EntityManager;

import org.osc.core.broker.job.Job;
import org.osc.core.broker.model.entities.appliance.DistributedAppliance;
import org.osc.core.broker.model.entities.virtualization.openstack.AvailabilityZone;
import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.broker.model.entities.virtualization.openstack.Host;
import org.osc.core.broker.model.entities.virtualization.openstack.HostAggregate;
import org.osc.core.broker.service.api.UpdateDeploymentSpecServiceApi;
import org.osc.core.broker.service.dto.openstack.AvailabilityZoneDto;
import org.osc.core.broker.service.dto.openstack.DeploymentSpecDto;
import org.osc.core.broker.service.dto.openstack.HostAggregateDto;
import org.osc.core.broker.service.dto.openstack.HostDto;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.DeploymentSpecEntityMgr;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.request.BaseRequest;
import org.osc.core.broker.service.response.BaseJobResponse;
import org.osc.core.broker.service.tasks.conformance.UnlockObjectMetaTask;
import org.osc.core.broker.service.validator.BaseDtoValidator;
import org.osc.core.broker.util.ValidateUtil;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class UpdateDeploymentSpecService
extends BaseDeploymentSpecService<BaseRequest<DeploymentSpecDto>, BaseJobResponse>
implements UpdateDeploymentSpecServiceApi {

    private static final Logger log = LoggerFactory.getLogger(UpdateDeploymentSpecService.class);

    @Reference
    private DeploymentSpecConformJobFactory dsConformJobFactory;

    private DeploymentSpec ds;

    @Override
    public BaseJobResponse exec(BaseRequest<DeploymentSpecDto> request, EntityManager em) throws Exception {
        validate(em, request.getDto());

        UnlockObjectMetaTask dsUnlock = null;

        try {
            DistributedAppliance da = this.ds.getVirtualSystem().getDistributedAppliance();
            dsUnlock = LockUtil.tryLockDS(this.ds, da, da.getApplianceManagerConnector(), this.ds.getVirtualSystem()
                    .getVirtualizationConnector());

            // as we do not allow to modify VS and IPPool for a DS we can assume they are the same.
            DeploymentSpecEntityMgr.toEntity(this.ds, request.getDto());
            if (this.vs.getVirtualizationConnector().getVirtualizationType().isOpenstack()) {
                if (!this.ds.getHosts().isEmpty() || !this.ds.getAvailabilityZones().isEmpty()
                        || !this.ds.getHostAggregates().isEmpty()) {
                    if (!this.ds.getAvailabilityZones().isEmpty()) {
                        this.ds.setAvailabilityZones(updateAvailabilityZones(em, request.getDto()
                                .getAvailabilityZones(), this.ds));
                    } else if (!this.ds.getHostAggregates().isEmpty()) {
                        this.ds.setHostAggregates(updateHostAggregates(em, request.getDto().getHostAggregates(),
                                this.ds));
                    } else if (!this.ds.getHosts().isEmpty()) {
                        this.ds.setHosts(updateHosts(em, request.getDto().getHosts(), this.ds));

                    }
                }
            }
            OSCEntityManager.update(em, this.ds, this.txBroadcastUtil);
            UnlockObjectMetaTask forLambda = dsUnlock;
            chain(() -> {
                try {
                    Job job = this.dsConformJobFactory.startDsConformanceJob(em, this.ds, forLambda);
                    return new BaseJobResponse(this.ds.getId(), job.getId());
                } catch (Exception e) {
                    LockUtil.releaseLocks(forLambda);
                    throw e;
                }
            });
        } catch (Exception e) {
            LockUtil.releaseLocks(dsUnlock);
            throw e;
        }

        return null;
    }

    @Override
    protected void validate(EntityManager em, DeploymentSpecDto dto) throws Exception {
        BaseDtoValidator.checkForNullId(dto);

        this.ds = em.find(DeploymentSpec.class, dto.getId());
        if (this.ds == null) {
            throw new VmidcBrokerValidationException("Deployment Specification with Id: " + dto.getId()
            + "  is not found.");
        }

        ValidateUtil.checkMarkedForDeletion(this.ds, this.ds.getName());

        super.validate(em, dto);

        if (!dto.getParentId().equals(this.ds.getVirtualSystem().getId())) {
            throwInvalidUpdateActionException("Virtual System", this.ds.getName());
        }
        if (this.ds.getVirtualSystem().getVirtualizationConnector().getVirtualizationType().isOpenstack()) {
            if (!dto.getProjectId().equals(this.ds.getProjectId())) {
                throwInvalidUpdateActionException("Project", this.ds.getName());
            } else if (dto.isShared() != this.ds.isShared()) {
                throwInvalidUpdateActionException("Shared", this.ds.getName());
            } else if (!dto.getRegion().equals(this.ds.getRegion())) {
                throwInvalidUpdateActionException("Region", this.ds.getName());
            } else if (!dto.getManagementNetworkId().equals(this.ds.getManagementNetworkId())) {
                throwInvalidUpdateActionException("Management Network Id", this.ds.getName());
            } else if (!dto.getInspectionNetworkId().equals(this.ds.getInspectionNetworkId())) {
                throwInvalidUpdateActionException("Inspection Network Id", this.ds.getName());
            } else if (isFloatingIpUpdated(dto)) {
                throwInvalidUpdateActionException("Floating Ip Pool", this.ds.getName());
            }
        }
    }

    private boolean isFloatingIpUpdated(DeploymentSpecDto dto) {
        if (null == dto.getFloatingIpPoolName() && null == this.ds.getFloatingIpPoolName()) {
            return false;
        } else {
            if (dto.getFloatingIpPoolName() != null) {
                return !dto.getFloatingIpPoolName().equals(this.ds.getFloatingIpPoolName());
            } else if (this.ds.getFloatingIpPoolName() != null) {
                return !this.ds.getFloatingIpPoolName().equals(dto.getFloatingIpPoolName());
            }
        }

        throw new IllegalStateException("Unable to determine if floating ip information has been updated!");
    }

    private Set<HostAggregate> updateHostAggregates(EntityManager em, Set<HostAggregateDto> selectedHaDtoSet,
            DeploymentSpec ds) {
        // assuming nothing changed
        Set<HostAggregate> updatedHaSet = new HashSet<>();
        updatedHaSet.addAll(ds.getHostAggregates());

        // Add selected ones
        for (HostAggregateDto selectedHa : selectedHaDtoSet) {
            if (!doesSetContainHostAggr(updatedHaSet, selectedHa)) {
                updatedHaSet.add(createHostAggregate(em, selectedHa, ds));
            }
        }

        Iterator<HostAggregate> dsHaIter = updatedHaSet.iterator();

        // Remove unselected ones
        while (dsHaIter.hasNext()) {
            HostAggregate dsHostAggr = dsHaIter.next();
            if (!doesSetContainHostAggr(selectedHaDtoSet, dsHostAggr)) {
                log.info("Deleting Host Aggregate :" + dsHostAggr.getOpenstackId());
                OSCEntityManager.delete(em, dsHostAggr, this.txBroadcastUtil);
                dsHaIter.remove();
            }
        }

        return updatedHaSet;
    }

    private boolean isEqual(HostAggregate hostAggrEntity, HostAggregateDto hostAggrDto) {
        return hostAggrEntity.getOpenstackId().equals(hostAggrDto.getOpenstackId());
    }

    private boolean doesSetContainHostAggr(Set<HostAggregateDto> selectedHaDtoSet, HostAggregate dsHostAggr) {
        for (HostAggregateDto haSetItem : selectedHaDtoSet) {
            if (isEqual(dsHostAggr, haSetItem)) {
                return true;
            }
        }
        return false;
    }

    private boolean doesSetContainHostAggr(Set<HostAggregate> updatedHaSet, HostAggregateDto selectedHa) {
        for (HostAggregate haSetItem : updatedHaSet) {
            if (isEqual(haSetItem, selectedHa)) {
                return true;
            }
        }
        return false;
    }

    private Set<AvailabilityZone> updateAvailabilityZones(EntityManager em, Set<AvailabilityZoneDto> selectedAZDtoSet,
            DeploymentSpec ds) {
        // assuming nothing changed
        Set<AvailabilityZone> updatedAzSet = new HashSet<>();
        updatedAzSet.addAll(ds.getAvailabilityZones());

        // Add selected ones
        for (AvailabilityZoneDto selectedAz : selectedAZDtoSet) {
            if (!doesSetContainAz(updatedAzSet, selectedAz)) {
                updatedAzSet.add(createAvailabilityZone(em, selectedAz, ds));
            }
        }

        Iterator<AvailabilityZone> dsAzIter = updatedAzSet.iterator();

        // Remove unselected ones
        while (dsAzIter.hasNext()) {
            AvailabilityZone dsAz = dsAzIter.next();
            if (!doesSetContainAz(selectedAZDtoSet, dsAz)) {
                log.info("Deleting Availability Zone:" + dsAz.getZone());
                OSCEntityManager.delete(em, dsAz, this.txBroadcastUtil);
                dsAzIter.remove();
            }
        }

        return updatedAzSet;
    }

    private boolean isEqual(AvailabilityZone az, AvailabilityZoneDto azDto) {
        return azDto.getRegion().equals(az.getRegion()) && azDto.getZone().equals(az.getZone());
    }

    private boolean doesSetContainAz(Set<AvailabilityZone> azSet, AvailabilityZoneDto az) {
        for (AvailabilityZone azSetItem : azSet) {
            if (isEqual(azSetItem, az)) {
                return true;
            }
        }
        return false;
    }

    private boolean doesSetContainAz(Set<AvailabilityZoneDto> azSet, AvailabilityZone az) {
        for (AvailabilityZoneDto azSetItem : azSet) {
            if (isEqual(az, azSetItem)) {
                return true;
            }
        }
        return false;
    }

    private Set<Host> updateHosts(EntityManager em, Set<HostDto> selectedHostDtoSet, DeploymentSpec ds) {
        // assuming nothing changed
        Set<Host> updatedHostSet = new HashSet<>();
        updatedHostSet.addAll(ds.getHosts());

        // Add selected ones
        for (HostDto selectedHost : selectedHostDtoSet) {
            if (!doesSetContainHost(updatedHostSet, selectedHost)) {
                updatedHostSet.add(createHost(em, selectedHost, ds));
            }
        }

        Iterator<Host> dsHostIter = updatedHostSet.iterator();

        // Remove unselected ones
        while (dsHostIter.hasNext()) {
            Host dsHost = dsHostIter.next();
            if (!doesSetContainHost(selectedHostDtoSet, dsHost)) {
                log.info("Deleting Host:" + dsHost.getName());
                OSCEntityManager.delete(em, dsHost, this.txBroadcastUtil);
                dsHostIter.remove();
            }
        }

        return updatedHostSet;
    }

    private boolean doesSetContainHost(Set<HostDto> hostDtoSet, Host hostToCheck) {
        for (HostDto hostSetItem : hostDtoSet) {
            if (hostSetItem.getOpenstackId().equals(hostToCheck.getOpenstackId())) {
                return true;
            }
        }
        return false;
    }

    private boolean doesSetContainHost(Set<Host> hostSet, HostDto hostToCheck) {
        for (Host hostSetItem : hostSet) {
            if (hostSetItem.getOpenstackId().equals(hostToCheck.getOpenstackId())) {
                return true;
            }
        }
        return false;
    }
}
