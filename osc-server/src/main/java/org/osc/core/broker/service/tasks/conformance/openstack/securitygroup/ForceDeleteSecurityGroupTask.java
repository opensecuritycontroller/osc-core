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
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMember;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMemberType;
import org.osc.core.broker.model.entities.virtualization.openstack.VMPort;
import org.osc.core.broker.service.persistence.OSCEntityManager;
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
    public void executeTransaction(EntityManager em) {
        log.debug("Force Deleting SecurityGroup Task : " + this.securityGroup.getName());
        // load Security Group from database to avoid Lazy loading issues
        this.securityGroup = SecurityGroupEntityMgr.findById(em, this.securityGroup.getId());

        // remove SGI - references
        for (SecurityGroupInterface sgi : this.securityGroup.getSecurityGroupInterfaces()) {
            sgi.removeSecurity(this.securityGroup);
            this.securityGroup.removeSecurityInterface(sgi);

            sgi.removeSecurity(this.securityGroup);

            // if this SGI is not referred by any other SG then remove this from Database as well.
            if (sgi.getSecurityGroups().isEmpty()) {
                // delete SGI from Database
                OSCEntityManager.delete(em, sgi);
            } else {
                // update SGI entity
                OSCEntityManager.update(em, sgi);
            }
        }

        // remove all SGM for this Security Group
        for (SecurityGroupMember sgm : this.securityGroup.getSecurityGroupMembers()) {

            // remove VM Ports for all SGM
            if (sgm.getType().equals(SecurityGroupMemberType.VM)) {
                for (VMPort port : sgm.getVm().getPorts()) {
                    port.removeAllDais();
                    OSCEntityManager.delete(em, port);
                }

                // remove VM from database
                OSCEntityManager.delete(em, sgm.getVm());

            }
            if (sgm.getType().equals(SecurityGroupMemberType.NETWORK)) {

                for (VMPort port : sgm.getNetwork().getPorts()) {
                    OSCEntityManager.delete(em, port);
                }

                // remove Network from database
                OSCEntityManager.delete(em, sgm.getNetwork());
            }

            if (sgm.getType().equals(SecurityGroupMemberType.SUBNET)) {

                for (VMPort port : sgm.getSubnet().getPorts()) {
                    OSCEntityManager.delete(em, port);
                }

                // remove Network from database
                OSCEntityManager.delete(em, sgm.getSubnet());
            }

            // remove SGM from database
            OSCEntityManager.delete(em, sgm);
        }

        // delete security group from database
        OSCEntityManager.delete(em, this.securityGroup);
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.securityGroup);
    }

}
