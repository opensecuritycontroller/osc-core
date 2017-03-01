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
import org.hibernate.Session;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.model.entities.management.Domain;
import org.osc.core.broker.model.entities.management.Policy;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;

public class CreatePolicyTask extends TransactionalTask {
    private static final Logger log = Logger.getLogger(CreatePolicyTask.class);

    private ApplianceManagerConnector mc;
    private Domain domain;
    private Policy policy;

    public CreatePolicyTask(ApplianceManagerConnector mc, Domain domain, Policy policy) {
        this.mc = mc;
        this.domain = domain;
        this.policy = policy;
        this.name = getName();
    }

    @Override
    public void executeTransaction(Session session) throws Exception {

        log.debug("Start excecuting CreatePolicyTask Task. Policy '" + policy.getName() + "'");
        mc = (ApplianceManagerConnector) session.get(ApplianceManagerConnector.class, mc.getId());

        Policy newPolicy = new Policy(mc, domain);
        newPolicy.setMgrPolicyId(policy.getMgrPolicyId());
        newPolicy.setName(policy.getName());
        domain.addPolicy(newPolicy);
        EntityManager.update(session, domain);
    }

    @Override
    public String getName() {
        return "Create Policy '" + policy.getName() + "' in Domain '" + domain.getName() + "'";
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(mc);
    }

}
