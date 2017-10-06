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
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osc.core.broker.util.log.LogProvider;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;

@Component(service=SyncMgrPublicKeyTask.class)
public class SyncMgrPublicKeyTask extends TransactionalTask {
    private static final Logger log = LogProvider.getLogger(SyncMgrPublicKeyTask.class);

    private ApplianceManagerConnector mc;
    private byte[] bytes;

    public SyncMgrPublicKeyTask create(ApplianceManagerConnector mc, byte[] bytes) {
        SyncMgrPublicKeyTask task = new SyncMgrPublicKeyTask();
        task.mc = mc;
        task.bytes = bytes;
        task.name = task.getName();
        task.dbConnectionManager = this.dbConnectionManager;
        task.txBroadcastUtil = this.txBroadcastUtil;

        return task;
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {
        log.debug("Start excecuting SyncMgrPublicKeyTask Task. MC: '" + this.mc.getName() + "'");

        this.mc = em.find(ApplianceManagerConnector.class, this.mc.getId(),
                LockModeType.PESSIMISTIC_WRITE);

        this.mc.setPublicKey(this.bytes);
        OSCEntityManager.update(em, this.mc, this.txBroadcastUtil);
    }

    @Override
    public String getName() {
        return "Syncing public key Manager Connector '" + this.mc.getName() + "'";
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.mc);
    }

}
