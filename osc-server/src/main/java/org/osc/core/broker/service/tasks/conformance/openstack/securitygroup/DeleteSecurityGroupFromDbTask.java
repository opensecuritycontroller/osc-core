/*******************************************************************************
 * Copyright (c) 2017 Intel Corporation
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

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupInterface;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;

/**
 * 
 * Deletes Security Group and removes the SG->SGI mappings
 * 
 */
public class DeleteSecurityGroupFromDbTask extends TransactionalTask {
    private static final Logger log = Logger.getLogger(DeleteSecurityGroupFromDbTask.class);

    private SecurityGroup sg;

    public DeleteSecurityGroupFromDbTask(SecurityGroup sg) {
        this.sg = sg;
        this.name = getName();
    }

    @Override
    public String getName() {
        return String.format("Delete Security Group '%s'", this.sg.getName());
    }

    @Override
    public void executeTransaction(Session session) {
        log.debug("Start Executing DeleteSecurityGroupFromDb Task : " + this.sg.getId());
        this.sg = (SecurityGroup) session.get(SecurityGroup.class, this.sg.getId());
        for (SecurityGroupInterface sgi : this.sg.getSecurityGroupInterfaces()) {
            sgi.removeSecurity(this.sg);
            this.sg.removeSecurityInterface(sgi);
            EntityManager.update(session, sgi);
            EntityManager.update(session, this.sg);
        }
        EntityManager.delete(session, this.sg);
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.sg);
    }

}
