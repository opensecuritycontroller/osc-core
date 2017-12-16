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
import org.osc.core.broker.job.Job.JobCompletionListener;
import org.osc.core.broker.job.JobEngine;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.DistributedAppliance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.service.api.DeleteDistributedApplianceServiceApi;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.request.BaseDeleteRequest;
import org.osc.core.broker.service.response.BaseJobResponse;
import org.osc.core.broker.service.tasks.conformance.UnlockObjectMetaTask;
import org.osc.core.broker.service.tasks.conformance.deleteda.DeleteDAFromDbTask;
import org.osc.core.broker.service.tasks.conformance.deleteda.ForceDeleteDATask;
import org.osc.core.broker.service.tasks.conformance.virtualsystem.VSConformanceCheckMetaTask;
import org.osc.core.broker.service.transactions.CompleteJobTransaction;
import org.osc.core.broker.service.transactions.CompleteJobTransactionInput;
import org.osc.core.broker.service.validator.DeleteDistributedApplianceRequestValidator;
import org.osc.core.broker.service.validator.RequestValidator;
import org.osc.core.common.job.TaskGuard;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This component exposes both the API and the implementation so that the
 * {@link DistributedApplianceConformJobFactory} can call the {@link #startDeleteDAJob(DistributedAppliance, UnlockObjectMetaTask)}
 * method.
 */
@Component(service = { DeleteDistributedApplianceServiceApi.class, DeleteDistributedApplianceService.class })
public class DeleteDistributedApplianceService extends ServiceDispatcher<BaseDeleteRequest, BaseJobResponse>
implements DeleteDistributedApplianceServiceApi {

    private static final Logger log = LoggerFactory.getLogger(DeleteDistributedApplianceService.class);
    private RequestValidator<BaseDeleteRequest, DistributedAppliance> validator;

    @Reference
    private ApiFactoryService apiFactoryService;

    @Reference
    VSConformanceCheckMetaTask vsConformanceCheckMetaTask;

    @Reference
    DeleteDAFromDbTask deleteDAFromDbTask;

    @Reference
    ForceDeleteDATask forceDeleteDATask;

    Job startDeleteDAJob(final DistributedAppliance da, UnlockObjectMetaTask ult) throws Exception {

        try {
            if (ult == null) {
                ult = LockUtil.tryLockDA(da, da.getApplianceManagerConnector());
            }

            Job job;
            TaskGraph tg = new TaskGraph();

            for (VirtualSystem vs : da.getVirtualSystems()) {
                TaskGraph vsDeleteTaskGraph = new TaskGraph();
                vsDeleteTaskGraph.appendTask(this.vsConformanceCheckMetaTask.create(vs));

                tg.addTaskGraph(vsDeleteTaskGraph);
            }

            // Only delete DA if all VS(s) were removed successfully.
            tg.appendTask(this.deleteDAFromDbTask.create(da), TaskGuard.ALL_ANCESTORS_SUCCEEDED);
            tg.appendTask(ult, TaskGuard.ALL_PREDECESSORS_COMPLETED);

            log.info("Start Submitting Delete DA Job");

            job = JobEngine.getEngine().submit("Delete Distributed Appliance '" + da.getName() + "'", tg,
                    LockObjectReference.getObjectReferences(da), new JobCompletionListener() {

                @Override
                public void completed(Job job) {
                    if (!job.getStatus().getStatus().isSuccessful()) {
                        try {
                            DeleteDistributedApplianceService.this.dbConnectionManager.getTransactionControl().required(() ->
                            new CompleteJobTransaction<DistributedAppliance>(DistributedAppliance.class, DeleteDistributedApplianceService.this.txBroadcastUtil)
                            .run(DeleteDistributedApplianceService.this.dbConnectionManager.getTransactionalEntityManager(), new CompleteJobTransactionInput(da.getId(), job.getId())));
                        } catch (Exception e) {
                            log.error("A serious error occurred in the Job Listener", e);
                            throw new RuntimeException("No Transactional resources are available", e);
                        }
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
    public BaseJobResponse exec(BaseDeleteRequest request, EntityManager em) throws Exception {
        if (this.validator == null) {
            this.validator = new DeleteDistributedApplianceRequestValidator(em);
        }

        UnlockObjectMetaTask ult = null;
        BaseJobResponse response = new BaseJobResponse();

        try {
            DistributedAppliance da = this.validator.validateAndLoad(request);
            ult = LockUtil.tryLockDA(da, da.getApplianceManagerConnector());
            if (request.isForceDelete()) {
                TaskGraph tg = new TaskGraph();
                tg.addTask(this.forceDeleteDATask.create(da));
                tg.appendTask(ult, TaskGuard.ALL_PREDECESSORS_COMPLETED);
                Job job = JobEngine.getEngine().submit("Force Delete Distributed Appliance '" + da.getName() + "'", tg,
                        LockObjectReference.getObjectReferences(da));
                response.setJobId(job.getId());

            } else {
                OSCEntityManager.markDeleted(em, da, this.txBroadcastUtil);
                UnlockObjectMetaTask forLambda = ult;
                chain(() -> {
                    try {
                        BaseJobResponse result = new BaseJobResponse();
                        // This is a new transaction so we need to get a live
                        // managed DA instance - if not then we can't read lazy fields
                        // and or may have invalid state
                        DistributedAppliance liveDA = em.find(DistributedAppliance.class, da.getId());
                        Job job = startDeleteDAJob(liveDA, forLambda);
                        result.setJobId(job.getId());
                        return result;
                    } catch (Exception e) {
                        LockUtil.releaseLocks(forLambda);
                        throw e;
                    }
                });
            }

        } catch (Exception ex) {
            LockUtil.releaseLocks(ult);
            throw ex;
        }

        return response;
    }
}
