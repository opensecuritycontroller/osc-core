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

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupInterface;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMember;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMemberType;
import org.osc.core.broker.model.entities.virtualization.openstack.VMPort;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.persistence.SecurityGroupEntityMgr;
import org.osc.core.broker.service.tasks.TransactionalTask;

/**
 *
 * Deletes Security Group and removes the SG->SGI mappings
 *
 */
public class ForceDeleteSecurityGroupTask extends TransactionalTask {
    private static final Logger log = Logger.getLogger(ForceDeleteSecurityGroupTask.class);

    private SecurityGroup securityGroup;

    public ForceDeleteSecurityGroupTask(SecurityGroup sg) {
        this.securityGroup = sg;
        this.name = getName();
    }

    @Override
    public String getName() {
        return String.format("Force Delete Security Group '%s'", this.securityGroup.getName());
    }

    @Override
    public void executeTransaction(Session session) {
        log.debug("Force Deleting SecurityGroup Task : " + this.securityGroup.getName());
        // load Security Group from database to avoid Lazy loading issues
        this.securityGroup = SecurityGroupEntityMgr.findById(session, this.securityGroup.getId());

        // remove SGI - references
        for (SecurityGroupInterface sgi : this.securityGroup.getSecurityGroupInterfaces()) {
            sgi.removeSecurity(this.securityGroup);
            this.securityGroup.removeSecurityInterface(sgi);

            sgi.removeSecurity(this.securityGroup);

            // if this SGI is not referred by any other SG then remove this from Database as well.
            if (sgi.getSecurityGroups().isEmpty()) {
                // delete SGI from Database
                EntityManager.delete(session, sgi);
            } else {
                // update SGI entity
                EntityManager.update(session, sgi);
            }
        }

        // remove all SGM for this Security Group
        for (SecurityGroupMember sgm : this.securityGroup.getSecurityGroupMembers()) {

            // remove VM Ports for all SGM
            if (sgm.getType().equals(SecurityGroupMemberType.VM)) {
                for (VMPort port : sgm.getVm().getPorts()) {
                    port.removeAllDais();
                    EntityManager.delete(session, port);
                }

                // remove VM from database
                EntityManager.delete(session, sgm.getVm());

            }
            if (sgm.getType().equals(SecurityGroupMemberType.NETWORK)) {

                for (VMPort port : sgm.getNetwork().getPorts()) {
                    EntityManager.delete(session, port);
                }

                // remove Network from database
                EntityManager.delete(session, sgm.getNetwork());
            }

            if (sgm.getType().equals(SecurityGroupMemberType.SUBNET)) {

                for (VMPort port : sgm.getSubnet().getPorts()) {
                    EntityManager.delete(session, port);
                }

                // remove Network from database
                EntityManager.delete(session, sgm.getSubnet());
            }

            // remove SGM from database
            EntityManager.delete(session, sgm);
        }

        // delete security group from database
        EntityManager.delete(session, this.securityGroup);
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.securityGroup);
    }

}
