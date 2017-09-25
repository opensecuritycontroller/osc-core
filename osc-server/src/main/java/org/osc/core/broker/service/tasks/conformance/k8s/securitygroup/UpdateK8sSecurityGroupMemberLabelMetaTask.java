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
import org.osgi.service.component.annotations.Component;

@Component(service = UpdateK8sSecurityGroupMemberLabelMetaTask.class)
public class UpdateK8sSecurityGroupMemberLabelMetaTask extends TransactionalMetaTask {

    private SecurityGroupMember sgm;

    @Override
    public TaskGraph getTaskGraph() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getName() {
        return String.format("Update Kubernetes Security Group Member Label %s", this.sgm.getLabel().getName());
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {
        // TODO : unimplemented
    }

    UpdateK8sSecurityGroupMemberLabelMetaTask create(SecurityGroupMember sgm) {
        UpdateK8sSecurityGroupMemberLabelMetaTask task = new UpdateK8sSecurityGroupMemberLabelMetaTask();
        task.sgm = sgm;
        task.dbConnectionManager = this.dbConnectionManager;
        task.txBroadcastUtil = this.txBroadcastUtil;

        return task;
    }
}
