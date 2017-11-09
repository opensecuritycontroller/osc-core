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
import org.osc.core.broker.job.JobQueuer;
import org.osc.core.broker.job.JobQueuer.JobRequest;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.job.JobRecord;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.persistence.SecurityGroupEntityMgr;
import org.osc.core.broker.service.tasks.conformance.UnlockObjectMetaTask;
import org.osc.core.broker.service.tasks.conformance.openstack.securitygroup.SecurityGroupCheckMetaTask;
import org.osc.core.broker.util.TransactionalBroadcastUtil;
import org.osc.core.broker.util.db.DBConnectionManager;
import org.osc.core.common.job.TaskGuard;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.transaction.control.ScopedWorkException;
import org.osgi.service.transaction.control.TransactionControl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = {SecurityGroupConformJobFactory.class})
public class SecurityGroupConformJobFactory {
    private static final Logger log = LoggerFactory.getLogger(SecurityGroupConformJobFactory.class);

    @Reference
    protected DBConnectionManager dbConnectionManager;

    @Reference
    protected TransactionalBroadcastUtil txBroadcastUtil;

    @Reference
    private SecurityGroupCheckMetaTask securityGroupCheckMetaTask;

    public Job startSecurityGroupConformanceJob(final SecurityGroup sg, UnlockObjectMetaTask sgUnlockTask)
            throws Exception {
        return startSecurityGroupConformanceJob(null, sg, sgUnlockTask, false);
    }

    /**
     * Starts a Security Group conformance job and executes unlock at the end.
     *
     * @param sg
     *            Security Group needs a Sync Job
     *
     * @param queueThisJob
     *            Will queue job in the queuer
     * @return
     *         The Sync Job
     *
     * @throws Exception
     */
    public Job startSecurityGroupConformanceJob(EntityManager em, final SecurityGroup sg,
            UnlockObjectMetaTask sgUnlockTask, boolean queueThisJob) throws Exception {
        TaskGraph tg = new TaskGraph();
        try {
            if (sgUnlockTask == null) {
                sgUnlockTask = LockUtil.tryLockSecurityGroup(sg, sg.getVirtualizationConnector());
            }
            tg.addTask(this.securityGroupCheckMetaTask.create(sg));
            tg.appendTask(sgUnlockTask, TaskGuard.ALL_PREDECESSORS_COMPLETED);

            String jobName = "Syncing Security Group";
            if (sg.getMarkedForDeletion()) {
                jobName = "Deleting Security Group";
            }
            if (queueThisJob) {
                JobQueuer.getInstance().putJob(new JobRequest(jobName + " '" + sg.getName() + "'", tg,
                        LockObjectReference.getObjectReferences(sg), getSecurityGroupJobCompletionListener(sg)));
                return null;
            }
            Job job = JobEngine.getEngine().submit(jobName + " '" + sg.getName() + "'", tg,
                    LockObjectReference.getObjectReferences(sg), getSecurityGroupJobCompletionListener(sg));

            updateSGJob(em, sg, job);

            return job;
        } catch (Exception e) {
            log.error("Fail to start SG conformance job.", e);
            LockUtil.releaseLocks(sgUnlockTask);
            throw e;
        }
    }

    public Job startBindSecurityGroupConformanceJob(EntityManager em, final SecurityGroup sg,
            UnlockObjectMetaTask sgUnlockTask)
                    throws Exception {

        TaskGraph tg = new TaskGraph();

        try {
            if (sgUnlockTask == null) {
                sgUnlockTask = LockUtil.tryLockSecurityGroup(sg, sg.getVirtualizationConnector());
            }
            tg.appendTask(this.securityGroupCheckMetaTask.create(sg));
            tg.appendTask(sgUnlockTask, TaskGuard.ALL_PREDECESSORS_COMPLETED);

            String jobName = "Bind Security Group Sync";
            if (sg.getMarkedForDeletion()) {
                jobName = "Deleting Security Group";
            }

            Job job = JobEngine.getEngine().submit(jobName + " '" + sg.getName() + "'", tg,
                    LockObjectReference.getObjectReferences(sg), getSecurityGroupJobCompletionListener(sg));

            updateSGJob(em, sg, job);

            return job;
        } catch (Exception e) {
            log.error("Fail to start Bind SG conformance job.", e);
            LockUtil.releaseLocks(sgUnlockTask);
            throw e;
        }
    }

    private JobCompletionListener getSecurityGroupJobCompletionListener(final SecurityGroup sg) {
        return new JobCompletionListener() {
            @Override
            public void completed(Job job) {
                SecurityGroupConformJobFactory.this.updateSGJob(null, sg, job);
            }
        };
    }

    public void updateSGJob(EntityManager em, final SecurityGroup sg, Job job) {

        // TODO: It would be more sensible to make this decision
        // using if(txControl.activeTransaction()) {...}

        if (em != null) {
            sg.setLastJob(em.find(JobRecord.class, job.getId()));
            OSCEntityManager.update(em, sg, this.txBroadcastUtil);
        } else {

            try {
                EntityManager txEm = this.dbConnectionManager.getTransactionalEntityManager();
                TransactionControl txControl = this.dbConnectionManager.getTransactionControl();
                txControl.required(() -> {
                    SecurityGroup securityGroupEntity = SecurityGroupEntityMgr.findById(txEm, sg.getId());
                    if (securityGroupEntity != null) {

                        securityGroupEntity = txEm.find(SecurityGroup.class, sg.getId());

                        securityGroupEntity.setLastJob(txEm.find(JobRecord.class, job.getId()));
                        OSCEntityManager.update(txEm, securityGroupEntity, this.txBroadcastUtil);
                    }
                    return null;
                });
            } catch (ScopedWorkException e) {
                // Unwrap the ScopedWorkException to get the cause from
                // the scoped work (i.e. the executeTransaction() call.
                log.error("Fail to update SG job status.", e.getCause());
            }
        }
    }

    /**
     * Starts a Security Group conformance job locks/unlock the sg after
     *
     */
    public Job startSecurityGroupConformanceJob(SecurityGroup sg) throws Exception {
        return startSecurityGroupConformanceJob(sg, null);
    }
}
