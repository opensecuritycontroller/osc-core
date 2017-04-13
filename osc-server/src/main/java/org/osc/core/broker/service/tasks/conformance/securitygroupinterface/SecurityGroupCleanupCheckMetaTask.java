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
package org.osc.core.broker.service.tasks.conformance.securitygroupinterface;

import java.util.Set;

import javax.persistence.EntityManager;

import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupInterface;
import org.osc.core.broker.service.tasks.TransactionalMetaTask;

public class SecurityGroupCleanupCheckMetaTask extends TransactionalMetaTask {

    private VirtualSystem vs;
    private TaskGraph tg;

    public SecurityGroupCleanupCheckMetaTask(VirtualSystem vs) {
        this.vs = vs;
        this.name = getName();
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {

        this.tg = new TaskGraph();

        this.vs = em.find(VirtualSystem.class, this.vs.getId());

        for (SecurityGroupInterface sgi : this.vs.getSecurityGroupInterfaces()) {
            this.tg.appendTask(new DeleteSecurityGroupInterfaceTask(sgi));
        }
    }

    @Override
    public String getName() {
        return "Cleaning Traffic Policy Mappings on Virtual System '" + this.vs.getVirtualizationConnector().getName()
                + "'";
    }

    @Override
    public TaskGraph getTaskGraph() {
        return this.tg;
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.vs);
    }

}
