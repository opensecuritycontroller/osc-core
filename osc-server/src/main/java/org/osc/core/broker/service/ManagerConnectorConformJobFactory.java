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
import org.osc.core.broker.model.entities.job.JobRecord;
import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.tasks.conformance.UnlockObjectTask;
import org.osc.core.broker.service.tasks.conformance.manager.MCConformanceCheckMetaTask;
import org.osc.core.broker.service.transactions.CompleteJobTransaction;
import org.osc.core.broker.service.transactions.CompleteJobTransactionInput;
import org.osc.core.broker.util.TransactionalBroadcastUtil;
import org.osc.core.broker.util.db.DBConnectionManager;
import org.osc.core.common.job.TaskGuard;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = {ManagerConnectorConformJobFactory.class})
public class ManagerConnectorConformJobFactory {

    private static final Logger log = LoggerFactory.getLogger(ManagerConnectorConformJobFactory.class);

    @Reference
    MCConformanceCheckMetaTask mcConformanceCheckMetaTask;

    @Reference
    protected DBConnectionManager dbConnectionManager;

    @Reference
    protected TransactionalBroadcastUtil txBroadcastUtil;

    /**
     * Starts and MC conform job and executes the unlock task at the end. If the unlock task is null then automatically
     * write locks the MC and release the lock at the end.
     * <p>
     * If a unlock task is provided, executes the unlock task at the end.
     * </p>
     */
    public Job startMCConformJob(final ApplianceManagerConnector mc, UnlockObjectTask mcUnlock, EntityManager em)
            throws Exception {

        log.info("Start MC (id:" + mc.getId() + ") Conformance Job");

        TaskGraph tg = new TaskGraph();

        tg.addTask(this.mcConformanceCheckMetaTask.create(mc, mcUnlock));
        if (mcUnlock != null) {
            tg.appendTask(mcUnlock, TaskGuard.ALL_PREDECESSORS_COMPLETED);
        }
        Job job = JobEngine.getEngine().submit("Syncing Appliance Manager Connector '" + mc.getName() + "'", tg,
                LockObjectReference.getObjectReferences(mc), new JobCompletionListener() {

            @Override
            public void completed(Job job) {
                try {
                    ManagerConnectorConformJobFactory.this.dbConnectionManager.getTransactionControl().required(() ->
                    new CompleteJobTransaction<ApplianceManagerConnector>(ApplianceManagerConnector.class,
                            ManagerConnectorConformJobFactory.this.txBroadcastUtil)
                    .run(ManagerConnectorConformJobFactory.this.dbConnectionManager.getTransactionalEntityManager(), new CompleteJobTransactionInput(mc.getId(), job.getId())));
                } catch (Exception e) {
                    log.error("A serious error occurred in the Job Listener", e);
                    throw new RuntimeException("No Transactional resources are available", e);
                }
            }
        });

        // Load MC on a new hibernate new session
        // ApplianceManagerConnector mc1 = (ApplianceManagerConnector) em.find(ApplianceManagerConnector.class,
        // mc.getId(),
        // new LockOptions(LockMode.PESSIMISTIC_WRITE));
        mc.setLastJob(em.find(JobRecord.class, job.getId()));
        OSCEntityManager.update(em, mc, this.txBroadcastUtil);

        log.info("Done submitting with jobId: " + job.getId());
        return job;
    }

    /**
     * Starts and MC conform job and locks/unlock the mc after
     */
    public Job startMCConformJob(final ApplianceManagerConnector mc, EntityManager em) throws Exception {
        return startMCConformJob(mc, null, em);
    }
}
