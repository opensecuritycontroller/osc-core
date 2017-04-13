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
import org.osc.core.broker.model.entities.virtualization.SecurityGroupInterface;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;

public class AddSecurityGroupTask extends TransactionalTask {
    //private static final Logger log = Logger.getLogger(AddSecurityGroupTask.class);

    private SecurityGroupInterface sgi;
    private String sgName;
    private String nsxSgId;

    public AddSecurityGroupTask(String sgName, String nsxSgId, SecurityGroupInterface sgi) {
        this.sgi = sgi;
        this.sgName = sgName;
        this.nsxSgId = nsxSgId;
        this.name = getName();
    }

    @Override
    public String getName() {
        return String.format("Creating Security Group '%s'" , this.sgName);
    }

    @Override
    public void executeTransaction(EntityManager em) {
        this.sgi = em.find(SecurityGroupInterface.class, this.sgi.getId());
        SecurityGroup sg = new SecurityGroup(this.sgi.getVirtualSystem().getVirtualizationConnector(), this.nsxSgId);
        sg.setName(this.sgName);
        sg.addSecurityGroupInterface(this.sgi);
        OSCEntityManager.create(em, sg);
        this.sgi.addSecurityGroup(sg);
        OSCEntityManager.update(em, this.sgi);
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.sgi);
    }

}
