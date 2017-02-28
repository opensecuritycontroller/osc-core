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

import org.apache.log4j.Logger;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Session;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;

public class SyncMgrPublicKeyTask extends TransactionalTask {
    private static final Logger log = Logger.getLogger(SyncMgrPublicKeyTask.class);

    private ApplianceManagerConnector mc;
    private byte[] bytes;

    public SyncMgrPublicKeyTask(ApplianceManagerConnector mc, byte[] bytes) {
        this.mc = mc;
        this.bytes = bytes;
        this.name = getName();
    }

    @Override
    public void executeTransaction(Session session) throws Exception {
        log.debug("Start excecuting SyncMgrPublicKeyTask Task. MC: '" + this.mc.getName() + "'");

        this.mc = (ApplianceManagerConnector) session.get(ApplianceManagerConnector.class, this.mc.getId(),
                new LockOptions(LockMode.PESSIMISTIC_WRITE));

        this.mc.setPublicKey(bytes);
        EntityManager.update(session, this.mc);
    }

    @Override
    public String getName() {
        return "Syncing public key Manager Connector '" + this.mc.getName() + "'";
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(mc);
    }

}
