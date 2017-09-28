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

import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMember;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMemberType;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osgi.service.component.annotations.Component;

@Component(service=AddSecurityGroupMemberTask.class)
public class AddSecurityGroupMemberTask extends TransactionalTask {
    //private static final Logger log = LoggerFactory.getLogger(AddSecurityGroupTask.class);

    private SecurityGroup securityGroup;
    private SecurityGroupMemberType type;
    private String address;

    public AddSecurityGroupMemberTask create(SecurityGroup securityGroup, SecurityGroupMemberType type, String address) {
        AddSecurityGroupMemberTask task = new AddSecurityGroupMemberTask();

        task.securityGroup = securityGroup;
        task.type = type;
        task.address = address;
        task.name = task.getName();
        task.dbConnectionManager = this.dbConnectionManager;
        task.txBroadcastUtil = this.txBroadcastUtil;

        return task;
    }

    @Override
    public String getName() {
        return String.format("Creating Security Group Member '%s' (%s) for Security Group '%s'", this.address,
                this.type, this.securityGroup.getName());
    }

    @Override
    public void executeTransaction(EntityManager em) {
        this.securityGroup = em.find(SecurityGroup.class, this.securityGroup.getId());
        SecurityGroupMember securityGroupMember = new SecurityGroupMember(this.securityGroup, this.type, this.address);
        OSCEntityManager.create(em, securityGroupMember, this.txBroadcastUtil);
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.securityGroup);
    }

}
