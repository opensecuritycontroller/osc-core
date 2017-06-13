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

import org.apache.log4j.Logger;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.model.plugin.manager.ManagerApiFactory;
import org.osc.core.broker.rest.server.OscAuthFilter;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osc.core.server.Server;
import org.osc.core.util.ServerUtil;
import org.osc.sdk.manager.api.ManagerCallbackNotificationApi;

public class RegisterMgrPolicyNotificationTask extends TransactionalTask {
    private static final Logger log = Logger.getLogger(RegisterMgrPolicyNotificationTask.class);

    private ApplianceManagerConnector mc;

    public RegisterMgrPolicyNotificationTask(ApplianceManagerConnector mc) {
        this.mc = mc;
        this.name = getName();
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {
        log.debug("Start excecuting RegisterMgrPolicyNotificationTask Task. MC: '" + this.mc.getName() + "'");

        this.mc = em.find(ApplianceManagerConnector.class, this.mc.getId(),
                LockModeType.PESSIMISTIC_WRITE);
        ManagerCallbackNotificationApi mgrApi = null;
        try {
            mgrApi = ManagerApiFactory.createManagerUrlNotificationApi(this.mc);
            mgrApi.createPolicyGroupNotificationRegistration(Server.getApiPort(), OscAuthFilter.OSC_DEFAULT_LOGIN,
                    OscAuthFilter.OSC_DEFAULT_PASS);
            this.mc.setLastKnownNotificationIpAddress(ServerUtil.getServerIP());
            OSCEntityManager.update(em, this.mc);
        } finally {
            if (mgrApi != null) {
                mgrApi.close();
            }
        }
    }

    @Override
    public String getName() {
        return "Register Policy Notifications for Manager Connector '" + this.mc.getName() + "'";
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.mc);
    }

}
