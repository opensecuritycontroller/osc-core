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
package org.osc.core.broker.service.tasks.conformance.manager;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.job.TaskGuard;
import org.osc.core.broker.job.lock.LockManager;
import org.osc.core.broker.job.lock.LockRequest;
import org.osc.core.broker.job.lock.LockRequest.LockType;
import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.model.plugin.manager.ManagerApiFactory;
import org.osc.core.broker.service.LockUtil;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.persistence.SslCertificateAttrEntityMgr;
import org.osc.core.broker.service.tasks.TransactionalMetaTask;
import org.osc.core.broker.service.tasks.conformance.UnlockObjectTask;
import org.osc.sdk.manager.api.ManagerCallbackNotificationApi;

public class MCDeleteMetaTask extends TransactionalMetaTask {
    private static final Logger log = Logger.getLogger(MCDeleteMetaTask.class);

    private ApplianceManagerConnector mc;
    private TaskGraph tg;

    public MCDeleteMetaTask(ApplianceManagerConnector mc) {
        this.mc = mc;
        this.name = getName();
    }

    @Override
    public void executeTransaction(Session session) throws Exception {

        this.tg = new TaskGraph();
        log.info("Start executing MCConformanceCheckMetaTask task for MC '" + this.mc.getName() + "'");

        UnlockObjectTask mcUnlockTask = null;
        try {
            this.mc = (ApplianceManagerConnector) session.get(ApplianceManagerConnector.class, this.mc.getId());

            mcUnlockTask = LockUtil.lockMC(this.mc, LockType.WRITE_LOCK);

            // Add Persisted Url Change Notification Subscription
            if (ManagerApiFactory.isPersistedUrlNotifications(mc)) {
                // Remove notification registrations

                try (ManagerCallbackNotificationApi mgrApi = ManagerApiFactory.createManagerUrlNotificationApi(mc)) {
                    if (mgrApi.getDomainNotificationRegistration() != null) {
                        mgrApi.deleteRegisteredDomain();
                    }
                    if (mgrApi.getPolicyGroupNotificationRegistration() != null) {
                        mgrApi.deleteRegisteredPolicyGroup();
                    }
                } catch (Exception ex) {
                    log.error("Fail to remove notification registration", ex);
                }
            }

            EntityManager<ApplianceManagerConnector> appMgrEntityMgr = new EntityManager<>(ApplianceManagerConnector.class, session);
            ApplianceManagerConnector connector = appMgrEntityMgr.findByPrimaryKey(this.mc.getId());

            SslCertificateAttrEntityMgr sslCertificateAttrEntityMgr = new SslCertificateAttrEntityMgr(session);
            sslCertificateAttrEntityMgr.removeCertificateList(connector.getSslCertificateAttrSet());

            appMgrEntityMgr.delete(this.mc.getId());

            if (this.tg.isEmpty()) {
                LockManager.getLockManager().releaseLock(new LockRequest(mcUnlockTask));
            } else {
                this.tg.appendTask(mcUnlockTask, TaskGuard.ALL_PREDECESSORS_COMPLETED);
            }

        } catch (Exception ex) {

            // If we experience any failure, unlock MC.
            if (mcUnlockTask != null) {
                log.info("Releasing lock for MC '" + this.mc.getName() + "'");
                LockManager.getLockManager().releaseLock(new LockRequest(mcUnlockTask));
            }

            throw ex;
        }
    }

    @Override
    public String getName() {
        return "Delete Appliance Manager Connector '" + this.mc.getName() + "'";
    }

    @Override
    public TaskGraph getTaskGraph() {
        return this.tg;
    }
}
