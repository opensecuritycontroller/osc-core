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

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.persistence.EntityManager;

import org.apache.log4j.Logger;
import org.osc.core.broker.job.Job;
import org.osc.core.broker.job.Job.JobCompletionListener;
import org.osc.core.broker.job.JobEngine;
import org.osc.core.broker.job.JobQueuer;
import org.osc.core.broker.job.JobQueuer.JobRequest;
import org.osc.core.broker.job.Task;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.job.lock.LockObjectReference.ObjectType;
import org.osc.core.broker.job.lock.LockRequest;
import org.osc.core.broker.model.entities.appliance.DistributedAppliance;
import org.osc.core.broker.model.entities.job.JobRecord;
import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.rest.client.openstack.openstack4j.Endpoint;
import org.osc.core.broker.service.api.ConformServiceApi;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.DeploymentSpecEntityMgr;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.persistence.SecurityGroupEntityMgr;
import org.osc.core.broker.service.request.ConformRequest;
import org.osc.core.broker.service.response.BaseJobResponse;
import org.osc.core.broker.service.tasks.conformance.DAConformanceCheckMetaTask;
import org.osc.core.broker.service.tasks.conformance.DowngradeLockObjectTask;
import org.osc.core.broker.service.tasks.conformance.UnlockObjectMetaTask;
import org.osc.core.broker.service.tasks.conformance.UnlockObjectTask;
import org.osc.core.broker.service.tasks.conformance.k8s.deploymentspec.ConformK8sDeploymentSpecMetaTask;
import org.osc.core.broker.service.tasks.conformance.manager.MCConformanceCheckMetaTask;
import org.osc.core.broker.service.tasks.conformance.openstack.deploymentspec.DSConformanceCheckMetaTask;
import org.osc.core.broker.service.tasks.conformance.openstack.securitygroup.SecurityGroupCheckMetaTask;
import org.osc.core.broker.service.tasks.conformance.securitygroupinterface.MgrSecurityGroupInterfacesCheckMetaTask;
import org.osc.core.broker.service.tasks.conformance.virtualizationconnector.CheckSSLConnectivityVcTask;
import org.osc.core.broker.service.transactions.CompleteJobTransaction;
import org.osc.core.broker.service.transactions.CompleteJobTransactionInput;
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

/**
 * This component exposes both the API and the implementation so that various
 * services can start conformance jobs. This could be removed if the relevant
 * methods could be added to the {@link ConformServiceApi}, but these method
 * signatures pull in external types.
 */
@Component(service = {ConformServiceApi.class, ConformService.class})
public class ConformService extends ServiceDispatcher<ConformRequest, BaseJobResponse> implements ConformServiceApi {
    private static final Logger log = Logger.getLogger(ConformService.class);

    @Reference
    private ApiFactoryService apiFactoryService;

    @Reference
    private DeleteDistributedApplianceService deleteDistributedApplianceService;

    @Reference
    private DAConformanceCheckMetaTask daConformanceCheckMetaTask;

    @Reference
    MCConformanceCheckMetaTask mcConformanceCheckMetaTask;

    @Reference
    private MgrSecurityGroupInterfacesCheckMetaTask mgrSecurityGroupInterfacesCheckMetaTask;

    @Reference
    private SecurityGroupCheckMetaTask securityGroupCheckMetaTask;

    @Reference
    private DowngradeLockObjectTask downgradeLockObjectTask;

    @Reference
    private CheckSSLConnectivityVcTask checkSSLConnectivityVcTask;

    @Reference
    private ConformK8sDeploymentSpecMetaTask conformK8sDeploymentSpecMetaTask;

    // optional+dynamic to resolve circular reference
    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile ServiceReference<DSConformanceCheckMetaTask> dsConformanceCheckMetaTaskSR;
    private DSConformanceCheckMetaTask dsConformanceCheckMetaTask;

    private final AtomicBoolean initDone = new AtomicBoolean();

    private BundleContext context;

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

    public Long startDAConformJob(EntityManager em, DistributedAppliance da) throws Exception {
        return startDAConformJob(em, da, null, true);
    }

    public Long startDAConformJob(EntityManager em, DistributedAppliance da, UnlockObjectMetaTask mcUnlock)
            throws Exception {
        return startDAConformJob(em, da, mcUnlock, true);
    }

    public Long startDAConformJob(EntityManager em, DistributedAppliance da, boolean trylock) throws Exception {
        return startDAConformJob(em, da, null, trylock);
    }

    public Long startDAConformJob(EntityManager em, DistributedAppliance da, UnlockObjectMetaTask daMcUnlockTask,
            boolean trylock) throws Exception {
        if (!da.getMarkedForDeletion()) {
            return startDASyncJob(em, da, daMcUnlockTask, trylock).getId();
        } else {
            return this.deleteDistributedApplianceService.startDeleteDAJob(da, null).getId();
        }
    }

    private Job startDASyncJob(EntityManager em, final DistributedAppliance da,
            UnlockObjectMetaTask daMcUnlockTask, boolean trylock) throws Exception {

        log.info("Start DA (id:" + da.getId() + ") Conformance Job");

        TaskGraph tg = new TaskGraph();

        try {
            ApplianceManagerConnector mc = da.getApplianceManagerConnector();
            if (daMcUnlockTask == null) {
                if (trylock) {
                    daMcUnlockTask = LockUtil.tryLockDA(da, mc);
                } else {
                    daMcUnlockTask = LockUtil.lockDA(da, mc);
                }
            }

            UnlockObjectTask mcReadUnlocktask = daMcUnlockTask
                    .getUnlockTaskByTypeAndId(ObjectType.APPLIANCE_MANAGER_CONNECTOR, mc.getId());
            UnlockObjectTask daWriteUnlocktask = daMcUnlockTask
                    .getUnlockTaskByTypeAndId(ObjectType.DISTRIBUTED_APPLIANCE, da.getId());

            Task mcCheck = this.mcConformanceCheckMetaTask.create(mc, mcReadUnlocktask);
            tg.addTask(mcCheck);
            tg.appendTask(this.downgradeLockObjectTask.create(new LockRequest(daWriteUnlocktask)),
                    TaskGuard.ALL_PREDECESSORS_COMPLETED);
            tg.appendTask(this.daConformanceCheckMetaTask.create(da), TaskGuard.ALL_PREDECESSORS_COMPLETED);

            // Sync MC security group interfaces only if the appliance manager supports policy mapping.
            if (this.apiFactoryService.syncsPolicyMapping(mc.getManagerType())) {
                tg.appendTask(this.mgrSecurityGroupInterfacesCheckMetaTask.create(da, mcReadUnlocktask),
                        TaskGuard.ALL_PREDECESSORS_COMPLETED);
            }

            tg.appendTask(daMcUnlockTask, TaskGuard.ALL_PREDECESSORS_COMPLETED);

            Job job = JobEngine.getEngine().submit("Syncing Distributed Appliance '" + da.getName() + "'", tg,
                    LockObjectReference.getObjectReferences(da), new JobCompletionListener() {

                @Override
                public void completed(Job job) {
                    try {
                        ConformService.this.dbConnectionManager.getTransactionControl().required(() ->
                        new CompleteJobTransaction<DistributedAppliance>(DistributedAppliance.class,
                                ConformService.this.txBroadcastUtil)
                        .run(ConformService.this.dbConnectionManager.getTransactionalEntityManager(), new CompleteJobTransactionInput(da.getId(), job.getId())));
                    } catch (Exception e) {
                        log.error("A serious error occurred in the Job Listener", e);
                        throw new RuntimeException("No Transactional resources are available", e);
                    }
                }
            });
            da.setLastJob(em.find(JobRecord.class, job.getId()));
            OSCEntityManager.update(em, da, this.txBroadcastUtil);

            try {
                List<DeploymentSpec> dss = DeploymentSpecEntityMgr.listDeploymentSpecByDistributedAppliance(em, da);
                for (DeploymentSpec ds : dss) {
                    ds.setLastJob(em.find(JobRecord.class, job.getId()));
                }
            } catch (Exception ex) {
                log.error("Fail to update DS job status.", ex);
            }

            log.info("Done submitting with jobId: " + job.getId());
            return job;

        } catch (Exception ex) {
            LockUtil.releaseLocks(daMcUnlockTask);
            throw ex;
        }
    }

    @Override
    public BaseJobResponse exec(ConformRequest request, EntityManager em) throws Exception {

        OSCEntityManager<DistributedAppliance> emgr = new OSCEntityManager<DistributedAppliance>(DistributedAppliance.class,
                em, this.txBroadcastUtil);
        DistributedAppliance da = emgr.findByPrimaryKey(request.getDaId());

        if (da == null) {
            throw new VmidcBrokerValidationException(
                    "Distributed Appliance with ID: " + request.getDaId() + ") was not found.");
        }

        if (da.getMarkedForDeletion()) {
            throw new VmidcBrokerValidationException(
                    "Syncing Distributed Appliance which is marked for deletion is not allowed.");
        }

        Long jobId = startDAConformJob(em, da);

        BaseJobResponse response = new BaseJobResponse();
        response.setJobId(jobId);

        return response;

    }

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
                    ConformService.this.dbConnectionManager.getTransactionControl().required(() ->
                    new CompleteJobTransaction<ApplianceManagerConnector>(ApplianceManagerConnector.class,
                            ConformService.this.txBroadcastUtil)
                    .run(ConformService.this.dbConnectionManager.getTransactionalEntityManager(), new CompleteJobTransactionInput(mc.getId(), job.getId())));
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

    /**
     * Starts and MC conform job and locks/unlock the mc after
     */
    public Job startMCConformJob(final ApplianceManagerConnector mc, EntityManager em) throws Exception {
        return startMCConformJob(mc, null, em);
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
                    ConformService.this.updateDSJob(null, ds, job);
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
                ConformService.this.updateSGJob(null, sg, job);
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
