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

import org.apache.log4j.Logger;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.model.entities.management.Domain;
import org.osc.core.broker.model.entities.management.Policy;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osgi.service.component.annotations.Component;

@Component(service = CreatePolicyTask.class)
public class CreatePolicyTask extends TransactionalTask {
    private static final Logger log = Logger.getLogger(CreatePolicyTask.class);

    private ApplianceManagerConnector mc;
    private Domain domain;
    private Policy policy;

    public CreatePolicyTask create(ApplianceManagerConnector mc, Domain domain, Policy policy) {
        CreatePolicyTask task = new CreatePolicyTask();
        task.mc = mc;
        task.domain = domain;
        task.policy = policy;
        task.name = task.getName();
        task.dbConnectionManager = this.dbConnectionManager;
        task.txBroadcastUtil = this.txBroadcastUtil;

        return task;
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {

        log.debug("Start excecuting CreatePolicyTask Task. Policy '" + this.policy.getName() + "'");
        this.mc = em.find(ApplianceManagerConnector.class, this.mc.getId());
        this.domain = em.find(Domain.class, this.domain.getId());

        Policy newPolicy = new Policy(this.mc, this.domain);
        newPolicy.setMgrPolicyId(this.policy.getMgrPolicyId());
        newPolicy.setName(this.policy.getName());
        this.domain.addPolicy(newPolicy);
        OSCEntityManager.update(em, this.domain, this.txBroadcastUtil);
    }

    @Override
    public String getName() {
        return "Create Policy '" + this.policy.getName() + "' in Domain '" + this.domain.getName() + "'";
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.mc);
    }

}
