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

import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.osc.core.broker.model.entities.appliance.VirtualSystemPolicy;
import org.osc.core.broker.model.entities.management.Policy;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.persistence.VirtualSystemPolicyEntityMgr;
import org.osc.core.broker.service.tasks.TransactionalTask;

public class DeletePolicyTask extends TransactionalTask {
    private static final Logger log = Logger.getLogger(DeletePolicyTask.class);

    private Policy policy;

    public DeletePolicyTask(Policy policy) {
        this.policy = policy;
        this.name = getName();
    }

    @Override
    public void executeTransaction(Session session) throws Exception {
        log.info("Start excecuting DeletePolicyTask Task. Policy '" + policy.getName() + "'");
        Long policyId = policy.getId();

        // If we've removed the last virtual system policies,
        // we can now delete the policy.

        List<VirtualSystemPolicy> vsps = VirtualSystemPolicyEntityMgr.listVSPolicyByPolicyId(session, policy.getId());
        if (vsps == null || vsps.isEmpty()) {
            log.info("Deleting policy '" + policy.getName() + "'");

            policy = (Policy) session.get(Policy.class, policyId);
            // We're assuming it is ok to delete the policy as Manager will ensure
            // it is not referenced by any security group.
            EntityManager.delete(session, policy);
        }

    }

    @Override
    public String getName() {
        return "Delete Policy '" + policy.getName() + "' from Domain '" + policy.getDomain().getName() + "'";
    }

}
