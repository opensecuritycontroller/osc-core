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
import org.osc.core.broker.job.JobEngine;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.job.TaskGuard;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.DistributedAppliance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.request.BaseDeleteRequest;
import org.osc.core.broker.service.response.BaseJobResponse;
import org.osc.core.broker.service.tasks.conformance.UnlockObjectMetaTask;
import org.osc.core.broker.service.tasks.conformance.deleteda.ForceDeleteVirtualSystemTask;

public class ForceDeleteVirtualSystemService extends ServiceDispatcher<BaseDeleteRequest, BaseJobResponse> {

    @Override
    public BaseJobResponse exec(BaseDeleteRequest request, EntityManager em) throws Exception {

        VirtualSystem vs = validate(em, request);

        UnlockObjectMetaTask ult = null;

        try {
            DistributedAppliance da = vs.getDistributedAppliance();
            ult = LockUtil.tryLockDA(da, da.getApplianceManagerConnector());
            TaskGraph tg = new TaskGraph();
            tg.addTask(new ForceDeleteVirtualSystemTask(vs));
            tg.appendTask(ult, TaskGuard.ALL_PREDECESSORS_COMPLETED);

            Job job = JobEngine.getEngine().submit("Force Delete Virtual System '" + vs.getName() + "'", tg,
                    LockObjectReference.getObjectReferences(da));

            BaseJobResponse response = new BaseJobResponse();
            response.setJobId(job.getId());
            return response;

        } catch (Exception ex) {
            LockUtil.releaseLocks(ult);
            throw ex;
        }
    }

    private VirtualSystem validate(EntityManager em, BaseDeleteRequest request) throws Exception {

        if (!request.isForceDelete()) {
            throw new VmidcBrokerValidationException("Virtual System can only be force deleted with this request");
        }

        VirtualSystem vs = em.find(VirtualSystem.class, request.getId());

        if (vs == null) {
            throw new VmidcBrokerValidationException("Virtual System with ID " + request.getId() + " is not found.");
        }

        if (!vs.getMarkedForDeletion() && request.isForceDelete()) {
            throw new VmidcBrokerValidationException("Virtual System '" + vs.getName() + "' (" + request.getId()
                    + ") is not marked for deletion and force delete operation is applicable only for entries marked for deletion.");
        }

        if (vs.getDistributedAppliance().getVirtualSystems().size() <= 1) {
            throw new VmidcBrokerValidationException("Virtual System '" + vs.getName() + "' (" + request.getId()
                    + ") is the only one in its DA. Deleting it will leave DA in invalid state. Please delete owning DA instead.");
        }

        return vs;
    }

}
