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

import javax.persistence.EntityManager;

import org.osc.core.broker.job.Job;
import org.osc.core.broker.job.Job.JobCompletionListener;
import org.osc.core.broker.job.JobEngine;
import org.osc.core.broker.job.Task;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.job.lock.LockObjectReference.ObjectType;
import org.osc.core.broker.job.lock.LockRequest;
import org.osc.core.broker.model.entities.appliance.DistributedAppliance;
import org.osc.core.broker.model.entities.job.JobRecord;
import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.service.persistence.DeploymentSpecEntityMgr;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.tasks.conformance.DAConformanceCheckMetaTask;
import org.osc.core.broker.service.tasks.conformance.DowngradeLockObjectTask;
import org.osc.core.broker.service.tasks.conformance.UnlockObjectMetaTask;
import org.osc.core.broker.service.tasks.conformance.UnlockObjectTask;
import org.osc.core.broker.service.tasks.conformance.manager.MCConformanceCheckMetaTask;
import org.osc.core.broker.service.tasks.conformance.securitygroupinterface.MgrSecurityGroupInterfacesCheckMetaTask;
import org.osc.core.broker.service.transactions.CompleteJobTransaction;
import org.osc.core.broker.service.transactions.CompleteJobTransactionInput;
import org.osc.core.broker.util.TransactionalBroadcastUtil;
import org.osc.core.broker.util.db.DBConnectionManager;
import org.osc.core.common.job.TaskGuard;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = {DistributedApplianceConformJobFactory.class})
public class DistributedApplianceConformJobFactory {
    private static final Logger log = LoggerFactory.getLogger(DistributedApplianceConformJobFactory.class);

    @Reference
    protected DBConnectionManager dbConnectionManager;

    @Reference
    protected TransactionalBroadcastUtil txBroadcastUtil;

    @Reference
    private ApiFactoryService apiFactoryService;

    @Reference
    private DeleteDistributedApplianceService deleteDistributedApplianceService;

    @Reference
    private DAConformanceCheckMetaTask daConformanceCheckMetaTask;

    @Reference
    MCConformanceCheckMetaTask mcConformanceCheckMetaTask;

    @Reference
    private DowngradeLockObjectTask downgradeLockObjectTask;

    @Reference
    private MgrSecurityGroupInterfacesCheckMetaTask mgrSecurityGroupInterfacesCheckMetaTask;

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
                        DistributedApplianceConformJobFactory.this.dbConnectionManager.getTransactionControl().required(() ->
                        new CompleteJobTransaction<DistributedAppliance>(DistributedAppliance.class,
                                DistributedApplianceConformJobFactory.this.txBroadcastUtil)
                        .run(DistributedApplianceConformJobFactory.this.dbConnectionManager.getTransactionalEntityManager(), new CompleteJobTransactionInput(da.getId(), job.getId())));
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
}
