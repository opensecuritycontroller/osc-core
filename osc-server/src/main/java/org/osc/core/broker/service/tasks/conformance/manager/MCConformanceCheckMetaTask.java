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
package org.osc.core.broker.service.tasks.conformance.manager;

import java.util.Arrays;
import java.util.Set;

import javax.persistence.EntityManager;

import org.apache.log4j.Logger;
import org.osc.core.broker.job.Task;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.job.TaskGuard;
import org.osc.core.broker.job.lock.LockManager;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.job.lock.LockRequest;
import org.osc.core.broker.job.lock.LockRequest.LockType;
import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.model.plugin.manager.ManagerApiFactory;
import org.osc.core.broker.model.plugin.manager.ManagerType;
import org.osc.core.broker.service.LockUtil;
import org.osc.core.broker.service.exceptions.VmidcBrokerInvalidRequestException;
import org.osc.core.broker.service.tasks.TransactionalMetaTask;
import org.osc.core.broker.service.tasks.conformance.DowngradeLockObjectTask;
import org.osc.core.broker.service.tasks.conformance.UnlockObjectTask;
import org.osc.core.broker.util.PasswordUtil;
import org.osc.core.util.ServerUtil;
import org.osc.sdk.manager.api.ApplianceManagerApi;
import org.osc.sdk.manager.api.ManagerCallbackNotificationApi;
import org.osc.sdk.manager.element.ManagerNotificationRegistrationElement;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = MCConformanceCheckMetaTask.class)
public class MCConformanceCheckMetaTask extends TransactionalMetaTask {
	private static final Logger log = Logger.getLogger(MCConformanceCheckMetaTask.class);

	private ApplianceManagerConnector mc;
	private TaskGraph tg;
	private UnlockObjectTask mcUnlockTask;

	@Reference
	ApiFactoryService apiFactoryService;

	@Reference
	private PasswordUtil passwordUtil;

	@Reference
	private RegisterMgrDomainNotificationTask registerMgrDomainNotificationTask;

	@Reference
	private RegisterMgrPolicyNotificationTask registerMgrPolicyNotificationTask;

	@Reference
	private UpdateMgrDomainNotificationTask updateMgrDomainNotificationTask;

	@Reference
	private UpdateMgrPolicyNotificationTask updateMgrPolicyNotificationTask;

	/**
	 * Start MC conformance task. A write lock exists for the duration of this task and any tasks kicked off by this
	 * task.
	 * <p>
	 * If the unlock task is provided, it automatically UPGRADES the lock to a write lock and will always DOWNGRADE the
	 * lock once the tasks are finished. The lock is NOT RELEASED and the provider of the lock should release the lock.
	 * </p>
	 * <p>
	 * If unlock task is not provided(null) then we acquire a write lock and RELEASE it after the tasks are finished.
	 * </p>
	 */
    public MCConformanceCheckMetaTask create(ApplianceManagerConnector mc, UnlockObjectTask mcUnlockTask) {
        MCConformanceCheckMetaTask task = new MCConformanceCheckMetaTask();
        task.mc = mc;
        task.mcUnlockTask = mcUnlockTask;
        task.name = task.getName();
        task.apiFactoryService = this.apiFactoryService;
        task.passwordUtil = this.passwordUtil;
        task.registerMgrDomainNotificationTask = this.registerMgrDomainNotificationTask;
        task.registerMgrPolicyNotificationTask = this.registerMgrPolicyNotificationTask;
        task.updateMgrDomainNotificationTask = this.updateMgrDomainNotificationTask;
        task.updateMgrPolicyNotificationTask = this.updateMgrPolicyNotificationTask;
        return task;
    }

	@Override
	public void executeTransaction(EntityManager em) throws Exception {
		boolean isLockProvided = true;
		this.tg = new TaskGraph();
		this.mc = em.find(ApplianceManagerConnector.class, this.mc.getId());

		try {
			this.mc = em.find(ApplianceManagerConnector.class, this.mc.getId());

			if (this.mcUnlockTask == null) {
				isLockProvided = false;
				this.mcUnlockTask = LockUtil.lockMC(this.mc, LockType.WRITE_LOCK);
			} else {
				// Upgrade to write lock. Will no op if we already have write lock
				boolean upgradeLockSucceeded = LockManager.getLockManager().upgradeLockWithWait(
						new LockRequest(this.mcUnlockTask));
				if (!upgradeLockSucceeded) {
					throw new VmidcBrokerInvalidRequestException("Fail to gain write lock for '"
							+ this.mcUnlockTask.getObjectRef().getType() + "' with name '"
							+ this.mcUnlockTask.getObjectRef().getName() + "'.");
				}
			}

			this.tg.addTaskGraph(syncPublicKey(em));
			if (this.apiFactoryService.isPersistedUrlNotifications(this.mc)) {
				this.tg.addTaskGraph(syncPersistedUrlNotification(em, this.mc));
			}

			if (this.apiFactoryService.syncsPolicyMapping(ManagerType.fromText(this.mc.getManagerType()))) {
				Task syncDomains = new SyncDomainMetaTask(this.mc);
				this.tg.addTask(syncDomains);

				Task syncPolicies = new SyncPolicyMetaTask(this.mc);
				this.tg.addTask(syncPolicies, syncDomains);
			}

			if (this.tg.isEmpty() && !isLockProvided) {
				LockManager.getLockManager().releaseLock(new LockRequest(this.mcUnlockTask));
			} else {
				if (isLockProvided) {
					//downgrade lock since we upgraded it at the start
					this.tg.appendTask(new DowngradeLockObjectTask(new LockRequest(this.mcUnlockTask)),
							TaskGuard.ALL_PREDECESSORS_COMPLETED);
				} else {
					this.tg.appendTask(this.mcUnlockTask, TaskGuard.ALL_PREDECESSORS_COMPLETED);
				}
			}

		} catch (Exception ex) {
			// If we experience any failure, unlock MC.
			if (this.mcUnlockTask != null && !isLockProvided) {
				log.info("Releasing lock for MC '" + this.mc.getName() + "'");
				LockManager.getLockManager().releaseLock(new LockRequest(this.mcUnlockTask));
			}
			throw ex;
		}
	}

	private TaskGraph syncPublicKey(EntityManager em) throws Exception {
		TaskGraph tg = new TaskGraph();

                ApplianceManagerApi applianceManagerApi = this.apiFactoryService.createApplianceManagerApi(ManagerType.fromText(this.mc.getManagerType()));
                byte[] bytes = applianceManagerApi.getPublicKey(this.apiFactoryService.getApplianceManagerConnectorElement(this.mc));

		if (bytes != null && (this.mc.getPublicKey() == null || !Arrays.equals(this.mc.getPublicKey(), bytes))) {
			tg.addTask(new SyncMgrPublicKeyTask(this.mc, bytes));
		}

		return tg;
	}

	public TaskGraph syncPersistedUrlNotification(EntityManager em, ApplianceManagerConnector mc)
			throws Exception {
		TaskGraph tg = new TaskGraph();

		ManagerCallbackNotificationApi mgrApi = null;
		try {
			mgrApi = ManagerApiFactory.createManagerUrlNotificationApi(mc);

			// Need to cache old broker ip because the manager tasks update the
			// LastKnownBrokerIp on the MC
			String oldIpAddress = mc.getLastKnownNotificationIpAddress();

			// Need to get policy group and domain registrations at the same time before we start adding the tasks
			// as the first task which gets executed updates the last known broker ip and the second update will not
			// see the registrations as out of sync
			ManagerNotificationRegistrationElement dr = mgrApi.getDomainNotificationRegistration();
			ManagerNotificationRegistrationElement pgr = mgrApi.getPolicyGroupNotificationRegistration();

			if (dr == null || dr.isEmpty()) {
				tg.addTask(this.registerMgrDomainNotificationTask.create(mc));
			} else {
				if (isOutOfSyncRegistration(dr)) {
					tg.appendTask(this.updateMgrDomainNotificationTask.create(mc, oldIpAddress));
				}
			}

			if (pgr == null || pgr.isEmpty()) {
				tg.appendTask(this.registerMgrPolicyNotificationTask.create(mc));
			} else {
				if (isOutOfSyncRegistration(pgr)) {
					tg.appendTask(this.updateMgrPolicyNotificationTask.create(mc, oldIpAddress));
				}
			}

		} finally {

			if (mgrApi != null) {
				mgrApi.close();
			}
		}

		return tg;
	}

    private boolean isOutOfSyncRegistration(ManagerNotificationRegistrationElement registration) {
        if (registration.getPassword() == null || !registration.getPassword().equals(this.passwordUtil.getOscDefaultPass())) {
            return true;
        }
        if (registration.getIpAddress() == null || !registration.getIpAddress().equals(ServerUtil.getServerIP())) {
            return true;
        }

        return false;
    }

    @Override
    public String getName() {
        return "Checking Appliance Manager Connector '" + this.mc.getName() + "'";
    }

    @Override
    public TaskGraph getTaskGraph() {
        return this.tg;
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.mc);
    }

}
