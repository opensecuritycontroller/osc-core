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

import java.util.HashSet;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;

import org.apache.log4j.Logger;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupInterface;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMember;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMemberType;
import org.osc.core.broker.model.entities.virtualization.openstack.VMPort;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;

/**
 * This base task represents the common responsibility between the tasks
 * {@see AllocateDAIWithSGIMembersTask} and {@see DeallocateDAIOfSGIMembersTask}
 * <p>
 * This task is applicable to SGIs whose virtual system refers to an SDN
 * controller that supports port groups.
 */
public abstract class UpdateDAIToSGIMembersTask extends TransactionalTask {
    private SecurityGroupInterface sgi;
    private DistributedApplianceInstance dai;
    private static final Logger LOG = Logger.getLogger(UpdateDAIToSGIMembersTask.class);

    public UpdateDAIToSGIMembersTask(SecurityGroupInterface sgi, DistributedApplianceInstance dai) {
        this.sgi = sgi;
        this.dai = dai;
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {
        this.sgi = em.find(SecurityGroupInterface.class, this.sgi.getId());
        this.dai = em.find(DistributedApplianceInstance.class, this.dai.getId(), LockModeType.PESSIMISTIC_WRITE);

        if (this.sgi.getSecurityGroup() == null || this.sgi.getSecurityGroup().getSecurityGroupMembers() == null) {
            LOG.info(String.format("The SGI %s security group does not have members.", this.sgi.getName()));
            return;
        }

        Set<VMPort> ports = new HashSet<>();
        for (SecurityGroupMember sgm : this.sgi.getSecurityGroup().getSecurityGroupMembers()) {
            // If SGM is marked for deletion, previous tasks should have removed the hooks and deleted the member from D.
            if (!sgm.getMarkedForDeletion()) {
                if (sgm.getType() == SecurityGroupMemberType.VM) {
                    ports.addAll(sgm.getVm().getPorts());
                } else if (sgm.getType() == SecurityGroupMemberType.NETWORK) {
                    ports.addAll(sgm.getNetwork().getPorts());
                } else if (sgm.getType() == SecurityGroupMemberType.SUBNET) {
                    ports.addAll(sgm.getSubnet().getPorts());
                }
            }
        }

        LOG.info(String.format("Retrieved %s ports in the SGI %s", ports.size(), this.sgi.getName()));

        for (VMPort port : ports) {
            updatePortProtection(port);
            OSCEntityManager.update(em, port, this.txBroadcastUtil);
            OSCEntityManager.update(em, this.dai, this.txBroadcastUtil);
        }
    }

    protected DistributedApplianceInstance getDai() {
        return this.dai;
    }

    protected SecurityGroupInterface getSGI() {
        return this.sgi;
    }

    /**
     * This method updates the provided port with the {@link #getDai()}
     *
     * @param protectedPort
     *            the port to be updated.
     */
    public abstract void updatePortProtection(VMPort protectedPort);

    public abstract UpdateDAIToSGIMembersTask create(SecurityGroupInterface sgi, DistributedApplianceInstance dai);
}
