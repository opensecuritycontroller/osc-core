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

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.osc.core.broker.job.Job;
import org.osc.core.broker.job.Job.JobCompletionListener;
import org.osc.core.broker.job.JobEngine;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.job.TaskGuard;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.DistributedAppliance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.request.BaseDeleteRequest;
import org.osc.core.broker.service.request.DeleteDistributedApplianceRequestValidator;
import org.osc.core.broker.service.request.RequestValidator;
import org.osc.core.broker.service.response.BaseJobResponse;
import org.osc.core.broker.service.tasks.conformance.UnlockObjectMetaTask;
import org.osc.core.broker.service.tasks.conformance.deleteda.DeleteDAFromDbTask;
import org.osc.core.broker.service.tasks.conformance.deleteda.ForceDeleteDATask;
import org.osc.core.broker.service.tasks.conformance.virtualsystem.VSConformanceCheckMetaTask;
import org.osc.core.broker.service.tasks.conformance.virtualsystem.ValidateNsxTask;
import org.osc.core.broker.service.transactions.CompleteJobTransaction;
import org.osc.core.broker.service.transactions.CompleteJobTransactionInput;
import org.osc.core.broker.util.TransactionalBroadcastUtil;
import org.osc.core.broker.util.db.TransactionalBrodcastListener;
import org.osc.core.broker.util.db.TransactionalRunner;

public class DeleteDistributedApplianceService extends ServiceDispatcher<BaseDeleteRequest, BaseJobResponse> {

    private static final Logger log = Logger.getLogger(DeleteDistributedApplianceService.class);
    private RequestValidator<BaseDeleteRequest, DistributedAppliance> validator;

    static Job startDeleteDAJob(final DistributedAppliance da, UnlockObjectMetaTask ult) throws Exception {

        try {
            if (ult == null) {
                ult = LockUtil.tryLockDA(da, da.getApplianceManagerConnector());
            }

            Job job;
            TaskGraph tg = new TaskGraph();

            for (VirtualSystem vs : da.getVirtualSystems()) {
                TaskGraph vsDeleteTaskGraph = new TaskGraph();
                if (vs.getVirtualizationConnector().getVirtualizationType().isVmware()) {
                    vsDeleteTaskGraph.addTask(new ValidateNsxTask(vs));
                }
                vsDeleteTaskGraph.appendTask(new VSConformanceCheckMetaTask(vs));

                tg.addTaskGraph(vsDeleteTaskGraph);
            }

            // Only delete DA if all VS(s) were removed successfully.
            tg.appendTask(new DeleteDAFromDbTask(da), TaskGuard.ALL_ANCESTORS_SUCCEEDED);
            tg.appendTask(ult, TaskGuard.ALL_PREDECESSORS_COMPLETED);

            log.info("Start Submitting Delete DA Job");

            job = JobEngine.getEngine().submit("Delete Distributed Appliance '" + da.getName() + "'", tg,
                    LockObjectReference.getObjectReferences(da), new JobCompletionListener() {

                        @Override
                        public void completed(Job job) {
                            if (!job.getStatus().isSuccessful()) {
                                new TransactionalRunner<Void, CompleteJobTransactionInput>(new TransactionalRunner.SharedSessionHandler())
                                        .withTransactionalListener(new TransactionalBrodcastListener())
                                        .exec(new CompleteJobTransaction<DistributedAppliance>(DistributedAppliance.class), new CompleteJobTransactionInput(da.getId(), job.getId()));
                            }

                        }
                    });
            log.info("Done submitting with jobId: " + job.getId());

            return job;

        } catch (Exception e) {
            LockUtil.releaseLocks(ult);
            throw e;
        }
    }

    @Override
    public BaseJobResponse exec(BaseDeleteRequest request, Session session) throws Exception {
        if (this.validator == null) {
            this.validator = new DeleteDistributedApplianceRequestValidator(session);
        }

        Long jobId;
        UnlockObjectMetaTask ult = null;
        BaseJobResponse response = new BaseJobResponse();

        try {
            DistributedAppliance da = this.validator.validateAndLoad(request);
            ult = LockUtil.tryLockDA(da, da.getApplianceManagerConnector());
            if (request.isForceDelete()) {
                TaskGraph tg = new TaskGraph();
                tg.addTask(new ForceDeleteDATask(da));
                tg.appendTask(ult, TaskGuard.ALL_PREDECESSORS_COMPLETED);
                Job job = JobEngine.getEngine().submit("Force Delete Distributed Appliance '" + da.getName() + "'", tg,
                        LockObjectReference.getObjectReferences(da));
                jobId = job.getId();

            } else {
                EntityManager.markDeleted(session, da);
                commitChanges(true);
                jobId = startDeleteDAJob(da, ult).getId();
            }
            response.setJobId(jobId);

        } catch (Exception ex) {
            LockUtil.releaseLocks(ult);
            TransactionalBroadcastUtil.removeSessionFromMap(session);
            throw ex;
        }

        return response;
    }
}
