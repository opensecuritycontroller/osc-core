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
package org.osc.core.broker.service;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.Session;
import org.osc.core.broker.job.Job;
import org.osc.core.broker.job.lock.LockRequest.LockType;
import org.osc.core.broker.model.entities.appliance.DistributedAppliance;
import org.osc.core.broker.model.entities.virtualization.openstack.AvailabilityZone;
import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.broker.model.entities.virtualization.openstack.Host;
import org.osc.core.broker.model.entities.virtualization.openstack.HostAggregate;
import org.osc.core.broker.service.dto.openstack.AvailabilityZoneDto;
import org.osc.core.broker.service.dto.openstack.DeploymentSpecDto;
import org.osc.core.broker.service.dto.openstack.HostAggregateDto;
import org.osc.core.broker.service.dto.openstack.HostDto;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.DeploymentSpecEntityMgr;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.request.BaseRequest;
import org.osc.core.broker.service.response.BaseJobResponse;
import org.osc.core.broker.service.tasks.conformance.UnlockObjectMetaTask;

public class AddDeploymentSpecService extends
        BaseDeploymentSpecService<BaseRequest<DeploymentSpecDto>, BaseJobResponse> {

    @Override
    public BaseJobResponse exec(BaseRequest<DeploymentSpecDto> request, Session session) throws Exception {

        BaseJobResponse response = new BaseJobResponse();

        UnlockObjectMetaTask unlockTask = null;
        validate(session, request.getDto());

        try {

            DistributedAppliance da = this.vs.getDistributedAppliance();
            unlockTask = LockUtil.tryReadLockDA(da, da.getApplianceManagerConnector());
            unlockTask
                    .addUnlockTask(LockUtil.tryLockVCObject(this.vs.getVirtualizationConnector(), LockType.READ_LOCK));

            DeploymentSpec ds = DeploymentSpecEntityMgr.createEntity(request.getDto(), this.vs);
            ds = EntityManager.create(session, ds);
            ds.setAvailabilityZones(createAvailabilityZones(ds, request.getDto(), session));
            ds.setHosts(createHosts(ds, request.getDto(), session));
            ds.setHostAggregates(createHostAggregates(ds, request.getDto(), session));

            commitChanges(true);

            // Lock the deployment spec with a write lock and allow it to be unlocked at the end of the job.
            unlockTask.addUnlockTask(LockUtil.tryLockDSOnly(ds));
            Job job = ConformService.startDsConformanceJob(session, ds, unlockTask);

            response.setJobId(job.getId());

            return response;
        } catch (Exception e) {
            LockUtil.releaseLocks(unlockTask);
            throw e;
        }
    }

    private Set<HostAggregate> createHostAggregates(DeploymentSpec ds, DeploymentSpecDto dto, Session session) {
        Set<HostAggregate> haSet = new HashSet<HostAggregate>();

        for (HostAggregateDto haDto : dto.getHostAggregates()) {
            haSet.add(createHostAggregate(session, haDto, ds));
        }
        return haSet;

    }

    @Override
    protected void validate(Session session, DeploymentSpecDto dto) throws Exception {
        super.validate(session, dto);

        if(dto.getInspectionNetworkId().equals(dto.getManagementNetworkId())) {
            throw new VmidcBrokerValidationException("Invalid Network Selection. Management and Inspection networks"
                    + " cannot be the same.");
        }

        DeploymentSpec existingDs = null;
        existingDs = DeploymentSpecEntityMgr.findDeploymentSpecByVirtualSystemTenantAndRegion(session, this.vs,
        				dto.getTenantId(), dto.getRegion());
        if (existingDs != null) {
            throw new VmidcBrokerValidationException("A Deployment Specification: " + existingDs.getName()
                    + " Already exists for the combination of the specified virtual system, tenant and region. "
                    + "Cannot add another Deployment Specification for the same combination.");
        }

    };

    private Set<AvailabilityZone> createAvailabilityZones(DeploymentSpec ds, DeploymentSpecDto dto, Session session) {
        Set<AvailabilityZone> azSet = new HashSet<AvailabilityZone>();

        for (AvailabilityZoneDto azDto : dto.getAvailabilityZones()) {
            azSet.add(createAvailabilityZone(session, azDto, ds));
        }
        return azSet;

    }

    private Set<Host> createHosts(DeploymentSpec ds, DeploymentSpecDto dto, Session session) {
        Set<Host> hsSet = new HashSet<Host>();

        for (HostDto hsDto : dto.getHosts()) {
            hsSet.add(createHost(session, hsDto, ds));
        }
        return hsSet;

    }
}
