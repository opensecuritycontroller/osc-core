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

import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;

import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.job.TaskGuard;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.plugin.manager.ManagerApiFactory;
import org.osc.core.broker.service.persistence.VirtualSystemEntityMgr;
import org.osc.core.broker.service.tasks.TransactionalMetaTask;
import org.osc.core.broker.service.tasks.conformance.securitygroupinterface.MgrSecurityGroupInterfacesCheckMetaTask;
import org.osc.core.broker.util.ValidateUtil;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = SecurityGroupCheckMetaTask.class)
public class SecurityGroupCheckMetaTask extends TransactionalMetaTask {

    @Reference
    MgrSecurityGroupInterfacesCheckMetaTask mgrSecurityGroupInterfacesCheckMetaTask;

    private SecurityGroup sg;
    private TaskGraph tg;

    public SecurityGroupCheckMetaTask create(SecurityGroup sg) {
        SecurityGroupCheckMetaTask task = new SecurityGroupCheckMetaTask();
        task.mgrSecurityGroupInterfacesCheckMetaTask = this.mgrSecurityGroupInterfacesCheckMetaTask;
        task.sg = sg;
        return task;
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {
        this.sg = em.find(SecurityGroup.class, this.sg.getId());

        this.tg = new TaskGraph();
        this.tg.addTask(new ValidateSecurityGroupTenantTask(this.sg));
        this.tg.appendTask(new SecurityGroupUpdateOrDeleteMetaTask(this.sg));

        if (!ValidateUtil.isEmpty(this.sg.getSecurityGroupInterfaces())) {
            List<VirtualSystem> referencedVs = VirtualSystemEntityMgr.listReferencedVSBySecurityGroup(em,
                    this.sg.getId());
            for (VirtualSystem vs : referencedVs) {
                if (vs.getMgrId() != null && ManagerApiFactory.syncsPolicyMapping(vs)) {
                    this.tg.appendTask(this.mgrSecurityGroupInterfacesCheckMetaTask.create(vs),
                            TaskGuard.ALL_PREDECESSORS_COMPLETED);
                }
            }
        }
    }

    @Override
    public String getName() {
        return String.format("Checking Security Group '%s'", this.sg.getName());
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
