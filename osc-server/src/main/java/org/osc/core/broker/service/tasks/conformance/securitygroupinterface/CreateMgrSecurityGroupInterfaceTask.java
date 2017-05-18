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

import org.apache.log4j.Logger;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupInterface;
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osc.sdk.manager.api.ManagerSecurityGroupInterfaceApi;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = CreateMgrSecurityGroupInterfaceTask.class)
public class CreateMgrSecurityGroupInterfaceTask extends TransactionalTask {
    private static final Logger log = Logger.getLogger(CreateMgrSecurityGroupInterfaceTask.class);

    @Reference
    private ApiFactoryService apiFactoryService;

    private SecurityGroupInterface securityGroupInterface;

    public CreateMgrSecurityGroupInterfaceTask create(SecurityGroupInterface securityGroup) {
        CreateMgrSecurityGroupInterfaceTask task = new CreateMgrSecurityGroupInterfaceTask();
        task.apiFactoryService = this.apiFactoryService;
        task.securityGroupInterface = securityGroup;
        task.name = task.getName();
        task.dbConnectionManager = this.dbConnectionManager;
        task.txBroadcastUtil = this.txBroadcastUtil;

        return task;
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {

        this.securityGroupInterface = em.find(SecurityGroupInterface.class,
                this.securityGroupInterface.getId());

        ManagerSecurityGroupInterfaceApi mgrApi = this.apiFactoryService
                .createManagerSecurityGroupInterfaceApi(this.securityGroupInterface.getVirtualSystem());
        try {
            String mgrSecurityGroupId = mgrApi.createSecurityGroupInterface(this.securityGroupInterface.getName(),
                    this.securityGroupInterface.getMgrPolicyId(), this.securityGroupInterface.getTag());
            log.info("Created Manager Security Group Interface '" + mgrSecurityGroupId + "'");

            this.securityGroupInterface.setMgrSecurityGroupIntefaceId(mgrSecurityGroupId);
            OSCEntityManager.update(em, this.securityGroupInterface, this.txBroadcastUtil);

        } finally {
            mgrApi.close();
        }

    }

    @Override
    public String getName() {
        return "Create Manager Security Group Interface '" + this.securityGroupInterface.getName()
                + "' of Virtualization System '"
                + this.securityGroupInterface.getVirtualSystem().getVirtualizationConnector().getName() + "'";
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.securityGroupInterface);
    }

}
