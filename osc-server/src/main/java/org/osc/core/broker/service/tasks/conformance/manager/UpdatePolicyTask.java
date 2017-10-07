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

import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.management.Policy;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.slf4j.LoggerFactory;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;

@Component(service=UpdatePolicyTask.class)
public class UpdatePolicyTask extends TransactionalTask {
    private static final Logger log = LoggerFactory.getLogger(UpdatePolicyTask.class);

    private Policy policy;
    private String newName;

    public UpdatePolicyTask create(Policy policy, String newName) {
        UpdatePolicyTask task = new UpdatePolicyTask();
        task.policy = policy;
        task.newName = newName;
        task.name = task.getName();
        task.dbConnectionManager = this.dbConnectionManager;
        task.txBroadcastUtil = this.txBroadcastUtil;

        return task;
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {

        log.debug("Start excecuting UpdatePolicyTask Task. Policy '" + this.policy.getName() + "'");
        this.policy = em.find(Policy.class, this.policy.getId());
        this.policy.setName(this.newName);
        OSCEntityManager.update(em, this.policy, this.txBroadcastUtil);
    }

    @Override
    public String getName() {
        return "Update Policy '" + this.policy.getName() + "' in Domain '" + this.policy.getDomain().getName() + "'";
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.policy.getApplianceManagerConnector());
    }

}
