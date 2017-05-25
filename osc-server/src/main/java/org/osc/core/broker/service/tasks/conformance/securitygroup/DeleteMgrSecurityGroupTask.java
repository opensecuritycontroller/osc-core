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
package org.osc.core.broker.service.tasks.conformance.securitygroup;

import java.util.Set;

import javax.persistence.EntityManager;

import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.plugin.manager.ManagerApiFactory;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osc.sdk.manager.api.ManagerSecurityGroupApi;
import org.osc.sdk.manager.element.ManagerSecurityGroupElement;
import org.osgi.service.component.annotations.Component;

@Component(service=DeleteMgrSecurityGroupTask.class)
public class DeleteMgrSecurityGroupTask extends TransactionalTask {

    private VirtualSystem vs;
    private ManagerSecurityGroupElement msge;

    public DeleteMgrSecurityGroupTask create(VirtualSystem vs, ManagerSecurityGroupElement msge) {
        DeleteMgrSecurityGroupTask task = new DeleteMgrSecurityGroupTask();
        task.vs = vs;
        task.msge = msge;
        task.name = task.getName();
        task.dbConnectionManager = this.dbConnectionManager;
        task.txBroadcastUtil = this.txBroadcastUtil;

        return task;
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {
        this.vs = em.find(VirtualSystem.class, this.vs.getId());

        ManagerSecurityGroupApi mgrApi = ManagerApiFactory.createManagerSecurityGroupApi(this.vs);
        try {
            mgrApi.deleteSecurityGroup(this.msge.getSGId());
        } finally {
            mgrApi.close();
        }
    }

    @Override
    public String getName() {
        return "Delete Manager Security Group '" + this.msge.getName() + "' (" + this.msge.getSGId()
                + ") of Virtualization System '" + this.vs.getVirtualizationConnector().getName() + "'";
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.vs);
    }

}
