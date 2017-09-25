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
package org.osc.core.broker.service.tasks.conformance.k8s.securitygroup;

import javax.persistence.EntityManager;

import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMember;
import org.osc.core.broker.service.tasks.TransactionalMetaTask;
import org.osc.core.broker.service.tasks.conformance.openstack.securitygroup.SecurityGroupMemberDeleteTask;
import org.osc.core.broker.service.tasks.conformance.securitygroup.MarkSecurityGroupMemberDeleteTask;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = CheckK8sSecurityGroupLabelMetaTask.class)
public class CheckK8sSecurityGroupLabelMetaTask extends TransactionalMetaTask {

    private TaskGraph tg;
    private SecurityGroupMember sgm;
    private boolean isDelete;

    @Reference
    UpdateK8sSecurityGroupMemberLabelMetaTask updateK8sSecurityGroupMemberLabelMetaTask;

    @Reference
    MarkSecurityGroupMemberDeleteTask markSecurityGroupMemberDeleteTask;

    @Reference
    SecurityGroupMemberDeleteTask securityGroupMemberDeleteTask;

    @Override
    public TaskGraph getTaskGraph() {
        return this.tg;
    }

    @Override
    public String getName() {
        return String.format("Conformance: %s Kubernetes Security Group Member Label %s",
                this.isDelete ? "Delete" : "Check", this.sgm.getLabel().getName());
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {
        this.tg = new TaskGraph();

        if (!this.isDelete) {
            this.tg.addTask(this.updateK8sSecurityGroupMemberLabelMetaTask.create(this.sgm, null));
        } else {
            this.tg.addTask(this.markSecurityGroupMemberDeleteTask.create(this.sgm));
            this.tg.appendTask(this.securityGroupMemberDeleteTask.create(this.sgm));
        }
    }

    public CheckK8sSecurityGroupLabelMetaTask create(SecurityGroupMember sgm, boolean isDelete) {
        CheckK8sSecurityGroupLabelMetaTask task = new CheckK8sSecurityGroupLabelMetaTask();
        task.sgm = sgm;
        task.isDelete = isDelete;
        task.updateK8sSecurityGroupMemberLabelMetaTask = this.updateK8sSecurityGroupMemberLabelMetaTask;
        task.markSecurityGroupMemberDeleteTask = this.markSecurityGroupMemberDeleteTask;
        task.securityGroupMemberDeleteTask = this.securityGroupMemberDeleteTask;
        task.dbConnectionManager = this.dbConnectionManager;
        task.txBroadcastUtil = this.txBroadcastUtil;

        return task;
    }

    @Override
    public String toString() {
        return "CheckK8sSecurityGroupLabelMetaTask [sgm=" + this.sgm + ", isDelete=" + this.isDelete + "]";
    }
}
