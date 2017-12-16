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

import javax.persistence.EntityManager;

import org.osc.core.broker.job.Job;
import org.osc.core.broker.job.lock.LockRequest.LockType;
import org.osc.core.broker.model.entities.appliance.DistributedAppliance;
import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.broker.service.api.SyncDeploymentSpecServiceApi;
import org.osc.core.broker.service.dto.openstack.DeploymentSpecDto;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.request.BaseRequest;
import org.osc.core.broker.service.response.BaseJobResponse;
import org.osc.core.broker.service.tasks.conformance.UnlockObjectMetaTask;
import org.osc.core.broker.util.ValidateUtil;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component
public class SyncDeploymentSpecService
extends BaseDeploymentSpecService<BaseRequest<DeploymentSpecDto>, BaseJobResponse>
implements SyncDeploymentSpecServiceApi {

    @Reference
    private DeploymentSpecConformJobFactory dsConformJobFactory;

    private DeploymentSpec ds;

    @Override
    public BaseJobResponse exec(BaseRequest<DeploymentSpecDto> request, EntityManager em) throws Exception {
        BaseJobResponse response = new BaseJobResponse();

        UnlockObjectMetaTask unlockTask = null;
        validate(em, request.getDto());

        try {
            DistributedAppliance da = this.ds.getVirtualSystem().getDistributedAppliance();
            unlockTask = LockUtil.tryReadLockDA(da, da.getApplianceManagerConnector());
            unlockTask.addUnlockTask(LockUtil.tryLockVCObject(this.ds.getVirtualSystem().getVirtualizationConnector(),
                    LockType.READ_LOCK));

            // Lock the DS with a write lock and allow it to be unlocked at the end of the job
            unlockTask.addUnlockTask(LockUtil.tryLockDSOnly(this.ds));
            Job job = this.dsConformJobFactory.startDsConformanceJob(em, this.ds, unlockTask);

            response.setJobId(job.getId());

            return response;
        } catch (Exception e) {
            LockUtil.releaseLocks(unlockTask);
            throw e;
        }
    }

    @Override
    protected void validate(EntityManager em, DeploymentSpecDto dto) throws Exception {
        this.ds = em.find(DeploymentSpec.class, dto.getId());
        if (this.ds == null) {
            throw new VmidcBrokerValidationException("Deployment Specification with Id: " + dto.getId()
            + "  is not found.");
        }

        ValidateUtil.checkMarkedForDeletion(this.ds, this.ds.getName());

    }
}
