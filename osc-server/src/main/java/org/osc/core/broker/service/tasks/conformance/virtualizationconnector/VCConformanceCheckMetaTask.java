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
package org.osc.core.broker.service.tasks.conformance.virtualizationconnector;

import org.apache.log4j.Logger;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.job.TaskGuard;
import org.osc.core.broker.job.lock.LockManager;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.job.lock.LockRequest;
import org.osc.core.broker.job.lock.LockRequest.LockType;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.service.LockUtil;
import org.osc.core.broker.service.exceptions.VmidcBrokerInvalidRequestException;
import org.osc.core.broker.service.tasks.TransactionalMetaTask;
import org.osc.core.broker.service.tasks.conformance.DowngradeLockObjectTask;
import org.osc.core.broker.service.tasks.conformance.UnlockObjectTask;
import org.osc.core.broker.service.vc.CheckSSLConnectivityVcTask;

import javax.persistence.EntityManager;
import java.util.Set;

public class VCConformanceCheckMetaTask extends TransactionalMetaTask {
    private static final Logger log = Logger.getLogger(VCConformanceCheckMetaTask.class);

    private VirtualizationConnector vc;
    private TaskGraph tg;
    private UnlockObjectTask vcUnlockTask;

    /**
     * Start VC conformance task. A write lock exists for the duration of this task and any tasks kicked off by this
     * task.
     * <p>
     * If the unlock task is provided, it automatically UPGRADES the lock to a write lock and will always DOWNGRADE the
     * lock once the tasks are finished. The lock is NOT RELEASED and the provider of the lock should release the lock.
     * </p>
     * <p>
     * If unlock task is not provided(null) then we acquire a write lock and RELEASE it after the tasks are finished.
     * </p>
     */
    public VCConformanceCheckMetaTask(VirtualizationConnector vc, UnlockObjectTask vcUnlockTask) {
        this.vc = vc;
        this.vcUnlockTask = vcUnlockTask;
        this.name = getName();
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {
        boolean isLockProvided = true;
        this.tg = new TaskGraph();

        try {
            this.vc = em.find(VirtualizationConnector.class, this.vc.getId());

            if (this.vcUnlockTask == null) {
                isLockProvided = false;
                this.vcUnlockTask = LockUtil.lockVC(this.vc, LockType.WRITE_LOCK);
            } else {
                // Upgrade to write lock. Will no op if we already have write lock
                boolean upgradeLockSucceeded = LockManager.getLockManager().upgradeLockWithWait(
                        new LockRequest(this.vcUnlockTask));
                if (!upgradeLockSucceeded) {
                    throw new VmidcBrokerInvalidRequestException("Fail to gain write lock for '"
                            + this.vcUnlockTask.getObjectRef().getType() + "' with name '"
                            + this.vcUnlockTask.getObjectRef().getName() + "'.");
                }
            }

            if(this.vc.isProviderHttps() || this.vc.getSslCertificateAttrSet().size() != 0) {
                this.tg.addTask(new CheckSSLConnectivityVcTask(this.vc));
            }

            if (this.tg.isEmpty() && !isLockProvided) {
                LockManager.getLockManager().releaseLock(new LockRequest(this.vcUnlockTask));
            } else {
                if (isLockProvided) {
                    //downgrade lock since we upgraded it at the start
                    this.tg.appendTask(new DowngradeLockObjectTask(new LockRequest(this.vcUnlockTask)),
                            TaskGuard.ALL_PREDECESSORS_COMPLETED);
                } else {
                    this.tg.appendTask(this.vcUnlockTask, TaskGuard.ALL_PREDECESSORS_COMPLETED);
                }
            }

        } catch (Exception ex) {
            // If we experience any failure, unlock VC.
            if (this.vcUnlockTask != null && !isLockProvided) {
                log.info("Releasing lock for VC '" + this.vc.getName() + "'");
                LockManager.getLockManager().releaseLock(new LockRequest(this.vcUnlockTask));
            }
            throw ex;
        }
    }

    @Override
    public String getName() {
        return "Checking Virtualization Connector '" + this.vc.getName() + "'";
    }

    @Override
    public TaskGraph getTaskGraph() {
        return this.tg;
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.vc);
    }
}