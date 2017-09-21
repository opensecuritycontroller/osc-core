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

import javax.persistence.EntityManager;

import org.apache.commons.lang.NotImplementedException;
import org.osc.core.broker.job.Task;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMember;
import org.osc.core.broker.service.tasks.TransactionalMetaTask;
import org.osgi.service.component.annotations.Component;

@Component(service = CheckK8sSecurityGroupLabelMetaTask.class)
public class CheckK8sSecurityGroupLabelMetaTask extends TransactionalMetaTask {

    private SecurityGroupMember sgm;
    private boolean isDelete;

    @Override
    public TaskGraph getTaskGraph() {
        // TODO unimplemented
        throw new NotImplementedException(getClass());
    }

    @Override
    public String getName() {
        return "Check Kubernetes Security Group Label Meta Task -- unimplemented " + this.sgm.getId();
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {
        // TODO Auto-generated method stub
    }

    public Task create(SecurityGroupMember sgm, boolean isDelete) {
        CheckK8sSecurityGroupLabelMetaTask task = new CheckK8sSecurityGroupLabelMetaTask();
        task.sgm = sgm;
        task.isDelete = isDelete;
        return task;
    }

    @Override
    public String toString() {
        return "CheckK8sSecurityGroupLabelMetaTask [sgm=" + this.sgm + ", isDelete=" + this.isDelete + "]";
    }
}
