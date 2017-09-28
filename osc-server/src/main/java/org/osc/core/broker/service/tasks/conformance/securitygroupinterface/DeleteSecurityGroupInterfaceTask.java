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
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupInterface;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osgi.service.component.annotations.Component;

/**
 *
 * Deletes Security Group Interface and removes the SG->SGI mappings
 *
 */
@Component(service = DeleteSecurityGroupInterfaceTask.class)
public class DeleteSecurityGroupInterfaceTask extends TransactionalTask {
    // private static final Logger log = LogProvider.getLogger(DeleteSecurityGroupInterfaceTask.class);

    private SecurityGroupInterface securityGroupInterface;

    public DeleteSecurityGroupInterfaceTask create(SecurityGroupInterface securityGroupInterface) {
        DeleteSecurityGroupInterfaceTask task = new DeleteSecurityGroupInterfaceTask();
        task.securityGroupInterface = securityGroupInterface;
        task.name = task.getName();
        task.dbConnectionManager = this.dbConnectionManager;
        task.txBroadcastUtil = this.txBroadcastUtil;

        return task;
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {

        this.securityGroupInterface = em.find(SecurityGroupInterface.class, this.securityGroupInterface.getId());

        SecurityGroup sg = this.securityGroupInterface.getSecurityGroup();
        sg.removeSecurityInterface(this.securityGroupInterface);

        OSCEntityManager.delete(em, this.securityGroupInterface, this.txBroadcastUtil);
    }

    @Override
    public String getName() {
        return "Delete Security Group Interface '" + this.securityGroupInterface.getName() + "' ("
                + this.securityGroupInterface.getTag() + ")";
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.securityGroupInterface);
    }

}
