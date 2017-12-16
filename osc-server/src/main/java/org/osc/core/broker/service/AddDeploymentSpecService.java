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
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;

import org.osc.core.broker.job.Job;
import org.osc.core.broker.job.lock.LockRequest.LockType;
import org.osc.core.broker.model.entities.appliance.DistributedAppliance;
import org.osc.core.broker.model.entities.virtualization.openstack.AvailabilityZone;
import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.broker.model.entities.virtualization.openstack.Host;
import org.osc.core.broker.model.entities.virtualization.openstack.HostAggregate;
import org.osc.core.broker.service.api.AddDeploymentSpecServiceApi;
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
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component
public class AddDeploymentSpecService extends BaseDeploymentSpecService<BaseRequest<DeploymentSpecDto>, BaseJobResponse>
implements AddDeploymentSpecServiceApi {

    @Reference
    private DeploymentSpecConformJobFactory dsConformJobFactory;

    @Override
    public BaseJobResponse exec(BaseRequest<DeploymentSpecDto> request, EntityManager em) throws Exception {

        UnlockObjectMetaTask unlockTask = null;
        validate(em, request.getDto());

        try {

            DistributedAppliance da = this.vs.getDistributedAppliance();
            unlockTask = LockUtil.tryReadLockDA(da, da.getApplianceManagerConnector());
            unlockTask
            .addUnlockTask(LockUtil.tryLockVCObject(this.vs.getVirtualizationConnector(), LockType.READ_LOCK));

            DeploymentSpec ds = DeploymentSpecEntityMgr.createEntity(request.getDto(), this.vs);
            OSCEntityManager.create(em, ds, this.txBroadcastUtil);
            ds.setAvailabilityZones(createAvailabilityZones(ds, request.getDto(), em));
            ds.setHosts(createHosts(ds, request.getDto(), em));
            ds.setHostAggregates(createHostAggregates(ds, request.getDto(), em));

            UnlockObjectMetaTask forLambda = unlockTask;
            chain(() -> {
                // Lock the deployment spec with a write lock and allow it to be unlocked at the end of the job.
                try {
                    forLambda.addUnlockTask(LockUtil.tryLockDSOnly(ds));

                    Job job = this.dsConformJobFactory.startDsConformanceJob(em, ds, forLambda);

                    return new BaseJobResponse(ds.getId(), job.getId());
                } catch (Exception e) {
                    LockUtil.releaseLocks(forLambda);
                    throw e;
                }
            });
        } catch (Exception e) {
            LockUtil.releaseLocks(unlockTask);
            throw e;
        }
        return null;
    }

    private Set<HostAggregate> createHostAggregates(DeploymentSpec ds, DeploymentSpecDto dto, EntityManager em) {
        Set<HostAggregate> haSet = new HashSet<HostAggregate>();

        for (HostAggregateDto haDto : dto.getHostAggregates()) {
            haSet.add(createHostAggregate(em, haDto, ds));
        }
        return haSet;
    }

    @Override
    protected void validate(EntityManager em, DeploymentSpecDto dto) throws Exception {
        super.validate(em, dto);

        if (this.vs.getVirtualizationConnector().getVirtualizationType().isOpenstack()) {
            if(dto.getInspectionNetworkId().equals(dto.getManagementNetworkId())) {
                throw new VmidcBrokerValidationException("Invalid Network Selection. Management and Inspection networks"
                        + " cannot be the same.");
            }

            DeploymentSpec existingDs = null;
            existingDs = DeploymentSpecEntityMgr.findDeploymentSpecByVirtualSystemProjectAndRegion(em, this.vs,
                    dto.getProjectId(), dto.getRegion());
            if (existingDs != null) {
                throw new VmidcBrokerValidationException("A Deployment Specification: " + existingDs.getName()
                + " Already exists for the combination of the specified virtual system, project and region. "
                + "Cannot add another Deployment Specification for the same combination.");
            }
        } else if (this.vs.getVirtualizationConnector().getVirtualizationType().isKubernetes()){
            List<DeploymentSpec> deploymentSpecs = DeploymentSpecEntityMgr.listDeploymentSpecByVirtualSystem(em, this.vs);

            if (deploymentSpecs != null && !deploymentSpecs.isEmpty()) {
                throw new VmidcBrokerValidationException("A deployment spec for the targed Kubernetes virtual system already exists.");
            }
        }
    }

    private Set<AvailabilityZone> createAvailabilityZones(DeploymentSpec ds, DeploymentSpecDto dto, EntityManager em) {
        Set<AvailabilityZone> azSet = new HashSet<AvailabilityZone>();

        for (AvailabilityZoneDto azDto : dto.getAvailabilityZones()) {
            azSet.add(createAvailabilityZone(em, azDto, ds));
        }
        return azSet;
    }

    private Set<Host> createHosts(DeploymentSpec ds, DeploymentSpecDto dto, EntityManager em) {
        Set<Host> hsSet = new HashSet<Host>();

        for (HostDto hsDto : dto.getHosts()) {
            hsSet.add(createHost(em, hsDto, ds));
        }
        return hsSet;
    }
}
