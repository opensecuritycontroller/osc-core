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
package org.osc.core.broker.service.tasks.conformance;

import java.util.Set;

import javax.persistence.EntityManager;

import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osc.core.broker.util.crypto.PKIUtil;
import org.slf4j.LoggerFactory;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;

@Component(service = GenerateVSSKeysTask.class)
public class GenerateVSSKeysTask extends TransactionalTask {
    private static final Logger log = LoggerFactory.getLogger(GenerateVSSKeysTask.class);

    private VirtualSystem vs;

    public GenerateVSSKeysTask create(VirtualSystem vs) {
        GenerateVSSKeysTask task = new GenerateVSSKeysTask();
        task.vs = vs;
        task.name = task.getName();
        task.dbConnectionManager = this.dbConnectionManager;
        task.txBroadcastUtil = this.txBroadcastUtil;

        return task;
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {

        log.debug("Start executing GetServiceInstanceTask");
        this.vs = em.find(VirtualSystem.class, this.vs.getId());

        // generate and persist keys
        this.vs.setKeyStore(PKIUtil.generateKeyStore());
        OSCEntityManager.update(em, this.vs, this.txBroadcastUtil);
    }

    @Override
    public String getName() {
        return "Register Service Instance '" + this.vs.getVirtualizationConnector().getName() + "'";
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.vs);
    }

}
