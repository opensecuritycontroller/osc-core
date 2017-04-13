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

import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupInterface;
import org.osc.core.broker.model.plugin.manager.ManagerApiFactory;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osc.sdk.manager.api.ManagerSecurityGroupInterfaceApi;

public class UpdateMgrSecurityGroupInterfaceTask extends TransactionalTask {
    //private static final Logger log = Logger.getLogger(UpdateMgrSecurityGroupInterfaceTask.class);

    private SecurityGroupInterface securityGroupInterface;

    public UpdateMgrSecurityGroupInterfaceTask(SecurityGroupInterface securityGroupInterface) {
        this.securityGroupInterface = securityGroupInterface;
        this.name = getName();
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {
        this.securityGroupInterface = (SecurityGroupInterface) em.find(SecurityGroupInterface.class,
                this.securityGroupInterface.getId());

        ManagerSecurityGroupInterfaceApi mgrApi = ManagerApiFactory
                .createManagerSecurityGroupInterfaceApi(this.securityGroupInterface.getVirtualSystem());
        try {
            mgrApi.updateSecurityGroupInterface(this.securityGroupInterface.getMgrSecurityGroupIntefaceId(),
                    this.securityGroupInterface.getName(), this.securityGroupInterface.getMgrPolicyId(),
                    this.securityGroupInterface.getTag());
        } finally {
            mgrApi.close();
        }
    }

    @Override
    public String getName() {
        return "Update Manager Security Group Interface '" + this.securityGroupInterface.getName() + "' ("
                + this.securityGroupInterface.getId() + ") of Virtualization System '"
                + this.securityGroupInterface.getVirtualSystem().getVirtualizationConnector().getName() + "'";
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.securityGroupInterface);
    }

}
