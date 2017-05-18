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

import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;

import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupInterface;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMember;
import org.osc.core.broker.model.plugin.sdncontroller.VMwareSdnApiFactory;
import org.osc.core.broker.service.persistence.SecurityGroupEntityMgr;
import org.osc.core.broker.service.request.ContainerSet;
import org.osc.core.broker.service.tasks.TransactionalMetaTask;
import org.osc.core.broker.service.tasks.conformance.openstack.securitygroup.DeleteSecurityGroupFromDbTask;
import org.osc.core.broker.service.tasks.conformance.openstack.securitygroup.SecurityGroupMemberDeleteTask;
import org.osc.sdk.sdn.api.ServiceProfileApi;
import org.osc.sdk.sdn.element.SecurityGroupElement;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service=NsxSecurityGroupsCheckMetaTask.class)
public class NsxSecurityGroupsCheckMetaTask extends TransactionalMetaTask {
    //private static final Logger log = Logger.getLogger(NsxSecurityGroupsCheckMetaTask.class);

    @Reference
    DeleteSecurityGroupFromDbTask deleteSecurityGroupFromDbTask;

    @Reference
    SecurityGroupMemberDeleteTask securityGroupMemberDeleteTask;

    @Reference
    NsxServiceProfileContainerCheckMetaTask nsxServiceProfileContainerCheckMetaTask;

    private VirtualSystem vs;
    private TaskGraph tg;

    public NsxSecurityGroupsCheckMetaTask create(VirtualSystem vs) {
        NsxSecurityGroupsCheckMetaTask task = new NsxSecurityGroupsCheckMetaTask();
        task.vs = vs;
        task.name = task.getName();
        task.deleteSecurityGroupFromDbTask = this.deleteSecurityGroupFromDbTask;
        task.securityGroupMemberDeleteTask = this.securityGroupMemberDeleteTask;
        task.nsxServiceProfileContainerCheckMetaTask = this.nsxServiceProfileContainerCheckMetaTask;
        task.dbConnectionManager = this.dbConnectionManager;
        task.txBroadcastUtil = this.txBroadcastUtil;

        return task;
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {
        this.tg = new TaskGraph();

        this.vs = em.find(VirtualSystem.class, this.vs.getId());
        ServiceProfileApi serviceProfileApi = VMwareSdnApiFactory.createServiceProfileApi(this.vs);
        for (SecurityGroupInterface sgi : this.vs.getSecurityGroupInterfaces()) {
            List<SecurityGroupElement> securityGroups = serviceProfileApi.getSecurityGroups(sgi.getTag());
            this.tg.appendTask(this.nsxServiceProfileContainerCheckMetaTask.create(sgi, new ContainerSet(securityGroups)));
        }

        List<SecurityGroup> unbindedSecurityGroups = SecurityGroupEntityMgr.listSecurityGroupsByVsAndNoBindings(
                em, this.vs);
        for (SecurityGroup sg : unbindedSecurityGroups) {
            for (SecurityGroupMember sgm : sg.getSecurityGroupMembers()) {
                this.tg.appendTask(this.securityGroupMemberDeleteTask.create(sgm));
            }
            this.tg.appendTask(this.deleteSecurityGroupFromDbTask.create(sg));
        }
    }

    @Override
    public String getName() {
        return "Checking Security Groups on Virtual System '" + this.vs.getVirtualizationConnector().getName() + "'";
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
