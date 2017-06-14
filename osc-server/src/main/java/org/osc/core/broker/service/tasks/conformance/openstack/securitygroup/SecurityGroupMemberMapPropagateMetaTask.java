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
package org.osc.core.broker.service.tasks.conformance.openstack.securitygroup;

import java.util.Set;

import javax.persistence.EntityManager;

import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupInterface;
import org.osc.core.broker.model.plugin.ApiFactoryService;
import org.osc.core.broker.service.tasks.TransactionalMetaTask;
import org.osc.core.broker.service.tasks.conformance.securitygroup.MgrSecurityGroupCheckMetaTask;
import org.osc.core.common.job.TaskGuard;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = SecurityGroupMemberMapPropagateMetaTask.class)
public class SecurityGroupMemberMapPropagateMetaTask extends TransactionalMetaTask {

    @Reference
    MgrSecurityGroupCheckMetaTask mgrSecurityGroupCheckMetaTask;

    @Reference
    private ApiFactoryService apiFactoryService;

    private SecurityGroup sg;

    private TaskGraph tg;

    public SecurityGroupMemberMapPropagateMetaTask create(SecurityGroup sg) {
        SecurityGroupMemberMapPropagateMetaTask task = new SecurityGroupMemberMapPropagateMetaTask();
        task.sg = sg;
        task.mgrSecurityGroupCheckMetaTask = this.mgrSecurityGroupCheckMetaTask;
        task.apiFactoryService = this.apiFactoryService;
        task.dbConnectionManager = this.dbConnectionManager;
        task.txBroadcastUtil = this.txBroadcastUtil;

        return task;
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {
        this.sg = em.find(SecurityGroup.class, this.sg.getId());

        this.tg = new TaskGraph();
        for (SecurityGroupInterface sgi : this.sg.getSecurityGroupInterfaces()) {
            VirtualSystem vs = sgi.getVirtualSystem();
            if (vs.getMgrId() != null  && this.apiFactoryService.syncsSecurityGroup(vs)) {
                // Sync SG members mapping to the manager directly
                this.tg.addTask(this.mgrSecurityGroupCheckMetaTask.create(vs, this.sg), TaskGuard.ALL_PREDECESSORS_COMPLETED);
            }
        }
    }

    @Override
    public String getName() {
        return String.format("Checking to propogate Security Group member list to appliances/managers '%s'", this.sg.getName());
    }

    @Override
    public TaskGraph getTaskGraph() {
        return this.tg;
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.sg);
    }

}
