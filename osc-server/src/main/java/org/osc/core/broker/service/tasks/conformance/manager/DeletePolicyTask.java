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

import java.util.List;

import javax.persistence.EntityManager;

import org.apache.log4j.Logger;
import org.osc.core.broker.model.entities.management.Policy;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupInterface;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.persistence.SecurityGroupInterfaceEntityMgr;
import org.osc.core.broker.service.tasks.TransactionalTask;

public class DeletePolicyTask extends TransactionalTask {
    private static final Logger log = Logger.getLogger(DeletePolicyTask.class);

    private Policy policy;

    public DeletePolicyTask(Policy policy) {
        this.policy = policy;
        this.name = getName();
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {
        log.info("Start excecuting DeletePolicyTask Task. Policy '" + this.policy.getName() + "'");
        Long policyId = this.policy.getId();

        // If we've removed the last security group interface,
        // we can now delete the policy.

        List<SecurityGroupInterface> sgis = SecurityGroupInterfaceEntityMgr.listSecurityGroupInterfaceByPolicy(em, this.policy.getId());
        if (sgis == null || sgis.isEmpty()) {
            log.info("Deleting policy '" + this.policy.getName() + "'");

            this.policy = em.find(Policy.class, policyId);
            // We're assuming it is ok to delete the policy as Manager will ensure
            // it is not referenced by any security group interface.
            OSCEntityManager.delete(em, this.policy);
        }

    }

    @Override
    public String getName() {
        return "Delete Policy '" + this.policy.getName() + "' from Domain '" + this.policy.getDomain().getName() + "'";
    }

}
