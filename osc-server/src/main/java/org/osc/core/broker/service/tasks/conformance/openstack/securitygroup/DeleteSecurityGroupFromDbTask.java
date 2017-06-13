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

import org.apache.log4j.Logger;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupInterface;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osgi.service.component.annotations.Component;

/**
 *
 * Deletes Security Group and removes the SG->SGI mappings
 *
 */
@Component(service = DeleteSecurityGroupFromDbTask.class)
public class DeleteSecurityGroupFromDbTask extends TransactionalTask {
    private static final Logger log = Logger.getLogger(DeleteSecurityGroupFromDbTask.class);

    private SecurityGroup sg;

    public DeleteSecurityGroupFromDbTask create(SecurityGroup sg) {
        DeleteSecurityGroupFromDbTask task = new DeleteSecurityGroupFromDbTask();
        task.sg = sg;
        task.name = task.getName();
        task.dbConnectionManager = this.dbConnectionManager;
        task.txBroadcastUtil = this.txBroadcastUtil;

        return task;
    }

    @Override
    public String getName() {
        return String.format("Delete Security Group '%s'", this.sg.getName());
    }

    @Override
    public void executeTransaction(EntityManager em) {
        log.debug("Start Executing DeleteSecurityGroupFromDb Task : " + this.sg.getId());
        this.sg = em.find(SecurityGroup.class, this.sg.getId());
        for (SecurityGroupInterface sgi : this.sg.getSecurityGroupInterfaces()) {
            sgi.removeSecurity(this.sg);
            this.sg.removeSecurityInterface(sgi);
            OSCEntityManager.update(em, sgi, this.txBroadcastUtil);
            OSCEntityManager.update(em, this.sg, this.txBroadcastUtil);
        }
        OSCEntityManager.delete(em, this.sg, this.txBroadcastUtil);
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.sg);
    }

}
