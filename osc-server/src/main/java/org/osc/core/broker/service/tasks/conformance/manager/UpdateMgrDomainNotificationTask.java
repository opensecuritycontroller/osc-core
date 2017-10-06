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

import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;

import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.service.api.RestConstants;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osc.core.broker.util.PasswordUtil;
import org.osc.core.broker.util.ServerUtil;
import org.osc.core.broker.util.log.LogProvider;
import org.osc.core.server.Server;
import org.osc.sdk.manager.api.ManagerCallbackNotificationApi;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;

@Component(service = UpdateMgrDomainNotificationTask.class)
public class UpdateMgrDomainNotificationTask extends TransactionalTask {
    private static final Logger log = LogProvider.getLogger(UpdateMgrDomainNotificationTask.class);

    @Reference
    private PasswordUtil passwordUtil;

    @Reference
    private ApiFactoryService apiFactoryService;

    private ApplianceManagerConnector mc;
    private String oldBrokerIp;

    public UpdateMgrDomainNotificationTask create(ApplianceManagerConnector mc, String oldBrokerIp) {
        UpdateMgrDomainNotificationTask task = new UpdateMgrDomainNotificationTask();
        task.mc = mc;
        task.name = task.getName();
        task.oldBrokerIp = oldBrokerIp;
        task.passwordUtil = this.passwordUtil;
        task.apiFactoryService = this.apiFactoryService;
        task.dbConnectionManager = this.dbConnectionManager;
        task.txBroadcastUtil = this.txBroadcastUtil;

        return task;
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {
        log.debug("Start excecuting RegisterMgrDomainNotificationTask Task. MC: '" + this.mc.getName() + "'");

        this.mc = em.find(ApplianceManagerConnector.class, this.mc.getId(),
                LockModeType.PESSIMISTIC_WRITE);
        ManagerCallbackNotificationApi mgrApi = null;
        try {
            mgrApi = this.apiFactoryService.createManagerUrlNotificationApi(this.mc);
            mgrApi.updateDomainNotificationRegistration(this.oldBrokerIp, Server.getApiPort(),
                    RestConstants.OSC_DEFAULT_LOGIN, this.passwordUtil.getOscDefaultPass());

            this.mc.setLastKnownNotificationIpAddress(ServerUtil.getServerIP());
            OSCEntityManager.update(em, this.mc, this.txBroadcastUtil);

        } finally {

            if (mgrApi != null) {
                mgrApi.close();
            }
        }
    }

    @Override
    public String getName() {
        return "Update Domain Notifications for Manager Connector '" + this.mc.getName() + "'";
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.mc);
    }

}
