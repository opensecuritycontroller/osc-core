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
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupInterface;
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osc.sdk.manager.api.ManagerSecurityGroupApi;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service=UpdateMgrSecurityGroupTask.class)
public class UpdateMgrSecurityGroupTask extends TransactionalTask {
    //private static final Logger log = LoggerFactory.getLogger(UpdateMgrSecurityGroupInterfaceTask.class);

    @Reference
    private ApiFactoryService apiFactoryService;

    private SecurityGroup sg;
    private SecurityGroupInterface sgi;
    private VirtualSystem vs;

    public UpdateMgrSecurityGroupTask create(VirtualSystem vs, SecurityGroup securityGroup, SecurityGroupInterface sgi) {
        UpdateMgrSecurityGroupTask task = new UpdateMgrSecurityGroupTask();
        task.vs = vs;
        task.sg = securityGroup;
        task.sgi = sgi;
        task.name = task.getName();
        task.apiFactoryService = this.apiFactoryService;
        task.dbConnectionManager = this.dbConnectionManager;
        task.txBroadcastUtil = this.txBroadcastUtil;

        return task;
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {
        this.sg = em.find(SecurityGroup.class, this.sg.getId());

        ManagerSecurityGroupApi mgrApi = this.apiFactoryService.createManagerSecurityGroupApi(this.vs);
        try {
			mgrApi.updateSecurityGroup(this.sgi.getMgrSecurityGroupId(), this.sg.getName(),
					CreateMgrSecurityGroupTask.getSecurityGroupMemberListElement(this.sg));
        } finally {
            mgrApi.close();
        }
    }

    @Override
    public String getName() {
        return "Update Manager Security Group '" + this.sg.getName() + " of Virtualization System '"
                + this.vs.getVirtualizationConnector().getName() + "'";
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.vs);
    }

}
