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
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.job.lock.LockRequest;
import org.osc.core.broker.model.entities.job.JobRecord;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.tasks.conformance.UnlockObjectTask;
import org.osc.core.broker.service.tasks.conformance.virtualizationconnector.CheckSSLConnectivityVcTask;
import org.osc.core.broker.service.transactions.CompleteJobTransaction;
import org.osc.core.broker.service.transactions.CompleteJobTransactionInput;
import org.osc.core.broker.util.TransactionalBroadcastUtil;
import org.osc.core.broker.util.db.DBConnectionManager;
import org.osc.core.common.job.TaskGuard;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = {VirtualizationConnectorConformJobFactory.class})
public class VirtualizationConnectorConformJobFactory {

    private static final Logger log = LoggerFactory.getLogger(VirtualizationConnectorConformJobFactory.class);

    @Reference
    protected DBConnectionManager dbConnectionManager;

    @Reference
    protected TransactionalBroadcastUtil txBroadcastUtil;

    @Reference
    private CheckSSLConnectivityVcTask checkSSLConnectivityVcTask;

    /**
     * Starts VC sync job and executes the unlock task at the end. If the unlock task is null then automatically
     * write locks the MC and release the lock at the end.
     * <p>
     * If a unlock task is provided, executes the unlock task at the end.
     * </p>
     */
    public Job startVCSyncJob(final VirtualizationConnector vc, EntityManager em)
            throws Exception {
        log.info("Start VC (id:" + vc.getId() + ") Synchronization Job");
        TaskGraph tg = new TaskGraph();
        UnlockObjectTask vcUnlockTask = LockUtil.lockVC(vc, LockRequest.LockType.READ_LOCK);
        tg.addTask(this.checkSSLConnectivityVcTask.create(vc));
        tg.appendTask(vcUnlockTask, TaskGuard.ALL_PREDECESSORS_COMPLETED);

        Job job = JobEngine.getEngine().submit("Syncing Virtualization Connector '" + vc.getName() + "'", tg,
                LockObjectReference.getObjectReferences(vc), job1 -> {
                    try {
                        this.dbConnectionManager.getTransactionControl().required(() ->
                        new CompleteJobTransaction<>(VirtualizationConnector.class,
                                this.txBroadcastUtil)
                        .run(this.dbConnectionManager.getTransactionalEntityManager(),
                                new CompleteJobTransactionInput(vc.getId(), job1.getId())));
                    } catch (Exception e) {
                        log.error("A serious error occurred in the Job Listener", e);
                        throw new RuntimeException("No Transactional resources are available", e);
                    }
                });

        vc.setLastJob(em.find(JobRecord.class, job.getId()));
        OSCEntityManager.update(em, vc, this.txBroadcastUtil);

        log.info("Done submitting with jobId: " + job.getId());
        return job;
    }
}
