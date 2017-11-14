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

import java.util.concurrent.atomic.AtomicBoolean;

import javax.persistence.EntityManager;

import org.osc.core.broker.job.Job;
import org.osc.core.broker.job.Job.JobCompletionListener;
import org.osc.core.broker.job.JobEngine;
import org.osc.core.broker.job.JobQueuer;
import org.osc.core.broker.job.JobQueuer.JobRequest;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.DistributedAppliance;
import org.osc.core.broker.model.entities.job.JobRecord;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.broker.rest.client.openstack.openstack4j.Endpoint;
import org.osc.core.broker.service.persistence.DeploymentSpecEntityMgr;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.tasks.conformance.UnlockObjectMetaTask;
import org.osc.core.broker.service.tasks.conformance.k8s.deploymentspec.ConformK8sDeploymentSpecMetaTask;
import org.osc.core.broker.service.tasks.conformance.openstack.deploymentspec.DSConformanceCheckMetaTask;
import org.osc.core.broker.util.TransactionalBroadcastUtil;
import org.osc.core.broker.util.db.DBConnectionManager;
import org.osc.core.common.job.TaskGuard;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.transaction.control.ScopedWorkException;
import org.osgi.service.transaction.control.TransactionControl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = {DeploymentSpecConformJobFactory.class})
public class DeploymentSpecConformJobFactory {
    private static final Logger log = LoggerFactory.getLogger(DeploymentSpecConformJobFactory.class);

    private final AtomicBoolean initDone = new AtomicBoolean();

    private BundleContext context;

    @Reference
    protected DBConnectionManager dbConnectionManager;

    @Reference
    protected TransactionalBroadcastUtil txBroadcastUtil;

    @Reference
    private ConformK8sDeploymentSpecMetaTask conformK8sDeploymentSpecMetaTask;

    // optional+dynamic to resolve circular reference
    //
    // Server needs DeploymentSpecConformJobFactory to come up, but Server is the one to register
    // RabbitMQRunner with the "active=true" property, and DeploymentSpecConformJobFactory
    // traces down to RabbitMQRunner"(active=true) dependency.
    //
    // The dependency chain is (as of today) Server -> DeploymentSpecConformJobFactory ->
    // DSConformanceCheckMetaTask -> DSUpdateOrDeleteMetaTask -> OsSvaCreateMetaTask ->
    // -> OsSvaServerCreateTask -> RabbitMQRunner.
    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile ServiceReference<DSConformanceCheckMetaTask> dsConformanceCheckMetaTaskSR;
    private DSConformanceCheckMetaTask dsConformanceCheckMetaTask;

    private void delayedInit() {
        if (this.initDone.compareAndSet(false, true)) {
            this.dsConformanceCheckMetaTask = this.context.getService(this.dsConformanceCheckMetaTaskSR);
        }
    }

    @Activate
    private void activate(BundleContext context) {
        this.context = context;
    }

    @Deactivate
    private void deactivate(BundleContext context) {
        if (this.initDone.get()) {
            context.ungetService(this.dsConformanceCheckMetaTaskSR);
        }
    }

    public Job startDsConformanceJob(DeploymentSpec ds, UnlockObjectMetaTask dsUnlockTask) throws Exception {
        return startDsConformanceJob(null, ds, dsUnlockTask, false);
    }

    public Job startDsConformanceJob(EntityManager em, DeploymentSpec ds, UnlockObjectMetaTask dsUnlockTask)
            throws Exception {
        return startDsConformanceJob(em, ds, dsUnlockTask, false);
    }

    /**
     *
     * Starts the DS Conformance job with appropriate locking.
     *
     * @param ds
     *            the deployment spec
     * @param queueThisJob
     *            Will queue this Job in the Job Queuer
     * @return the job
     *         will return Job ID if queueThisJob is false
     *         will return null is queueThisJob is true
     *
     * @throws Exception
     *
     */
    private Job startDsConformanceJob(EntityManager em, final DeploymentSpec ds,
            UnlockObjectMetaTask dsUnlockTask, boolean queueThisJob) throws Exception {
        delayedInit();
        TaskGraph tg = new TaskGraph();
        VirtualizationConnector vc = ds.getVirtualSystem().getVirtualizationConnector();
        DistributedAppliance da = ds.getVirtualSystem().getDistributedAppliance();
        try {
            if (dsUnlockTask == null) {
                dsUnlockTask = LockUtil.tryLockDS(ds, da, da.getApplianceManagerConnector(),
                        ds.getVirtualSystem().getVirtualizationConnector());
            }

            if (ds.getVirtualSystem().getVirtualizationConnector().getVirtualizationType().isOpenstack()) {
                tg.addTask(this.dsConformanceCheckMetaTask.create(ds, new Endpoint(vc, ds.getProjectName())));
            } else {
                tg.addTask(this.conformK8sDeploymentSpecMetaTask.create(ds));
            }

            tg.appendTask(dsUnlockTask, TaskGuard.ALL_PREDECESSORS_COMPLETED);

            String jobName = "Syncing Deployment Specification";
            if (ds.getMarkedForDeletion()) {
                jobName = "Deleting Deployment Specification";
            }
            if (queueThisJob) {
                JobQueuer.getInstance().putJob(new JobRequest(jobName + " '" + ds.getName() + "'", tg,
                        LockObjectReference.getObjectReferences(ds)));
                return null;
            }
            Job job = JobEngine.getEngine().submit(jobName + " '" + ds.getName() + "'", tg,
                    LockObjectReference.getObjectReferences(ds), new JobCompletionListener() {

                @Override
                public void completed(Job job) {
                    DeploymentSpecConformJobFactory.this.updateDSJob(null, ds, job);
                }
            });

            updateDSJob(em, ds, job);

            log.info("Done submitting with jobId: " + job.getId());
            return job;

        } catch (Exception ex) {
            log.error("Fail to start DS conformance job.", ex);
            LockUtil.releaseLocks(dsUnlockTask);
            throw ex;
        }
    }

    private void updateDSJob(EntityManager em, final DeploymentSpec ds, Job job) {

        // TODO: It would be more sensible to make this decision
        // using if(txControl.activeTransaction()) {...}

        if (em != null) {
            ds.setLastJob(em.find(JobRecord.class, job.getId()));
            OSCEntityManager.update(em, ds, this.txBroadcastUtil);

        } else {

            try {
                EntityManager txEm = this.dbConnectionManager.getTransactionalEntityManager();
                TransactionControl txControl = this.dbConnectionManager.getTransactionControl();
                txControl.required(() -> {
                    DeploymentSpec ds1 = DeploymentSpecEntityMgr.findById(txEm, ds.getId());
                    if (ds1 != null) {
                        ds1.setLastJob(txEm.find(JobRecord.class, job.getId()));
                        OSCEntityManager.update(txEm, ds1, this.txBroadcastUtil);
                    }
                    return null;
                });
            } catch (ScopedWorkException e) {
                // Unwrap the ScopedWorkException to get the cause from
                // the scoped work (i.e. the executeTransaction() call.
                log.error("Fail to update DS job status.", e.getCause());
            }
        }
    }
}
