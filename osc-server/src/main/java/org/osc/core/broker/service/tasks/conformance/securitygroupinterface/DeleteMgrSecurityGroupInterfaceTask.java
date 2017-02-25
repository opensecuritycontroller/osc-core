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
package org.osc.core.broker.service.tasks.conformance.securitygroupinterface;

import java.util.Set;

import org.hibernate.Session;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.plugin.manager.ManagerApiFactory;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osc.sdk.manager.api.ManagerSecurityGroupInterfaceApi;
import org.osc.sdk.manager.element.ManagerSecurityGroupInterfaceElement;

public class DeleteMgrSecurityGroupInterfaceTask extends TransactionalTask {
    //private static final Logger log = Logger.getLogger(DeleteMgrSecurityGroupInterfaceTask.class);

    private VirtualSystem vs;
    private ManagerSecurityGroupInterfaceElement mgrSecurityGroupInterface;

    public DeleteMgrSecurityGroupInterfaceTask(VirtualSystem vs,
            ManagerSecurityGroupInterfaceElement mgrSecurityGroupInterface) {

        this.vs = vs;
        this.mgrSecurityGroupInterface = mgrSecurityGroupInterface;
        this.name = getName();
    }

    @Override
    public void executeTransaction(Session session) throws Exception {
        this.vs = (VirtualSystem) session.get(VirtualSystem.class, this.vs.getId());

        ManagerSecurityGroupInterfaceApi mgrApi = ManagerApiFactory.createManagerSecurityGroupInterfaceApi(this.vs);
        if (this.mgrSecurityGroupInterface.getSecurityGroupInterfaceId() != null) {
            try {
                mgrApi.deleteSecurityGroupInterface(this.mgrSecurityGroupInterface.getSecurityGroupInterfaceId());
            } finally {
                mgrApi.close();
            }
        }
    }

    @Override
    public String getName() {
        return "Delete Manager Security Group Interface '" + this.mgrSecurityGroupInterface.getName() + "' ("
                + this.mgrSecurityGroupInterface.getSecurityGroupInterfaceId() + ") of Virtualization System '"
                + this.vs.getVirtualizationConnector().getName() + "'";
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.vs);
    }

}
