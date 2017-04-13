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

import org.jboss.logging.Logger;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMember;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMemberType;
import org.osc.core.broker.model.entities.virtualization.openstack.Network;
import org.osc.core.broker.model.entities.virtualization.openstack.Subnet;
import org.osc.core.broker.model.entities.virtualization.openstack.VM;
import org.osc.core.broker.model.entities.virtualization.openstack.VMPort;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;

public class SecurityGroupMemberDeleteTask extends TransactionalTask {

    private final Logger log = Logger.getLogger(SecurityGroupMemberDeleteTask.class);

    private SecurityGroupMember sgm;

    public SecurityGroupMemberDeleteTask(SecurityGroupMember sgm) {
        this.sgm = sgm;
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {
        this.sgm = em.find(SecurityGroupMember.class, this.sgm.getId());

        if (this.sgm.getType() == SecurityGroupMemberType.VM) {
            VM vm = this.sgm.getVm();

            if (vm.getSecurityGroupMembers().size() == 1) {
                // VM has reference only to this SGM. Delete the vm too.
                this.log.info("No other references to VM found. Deleting VM " + vm.getName());
                int portsDeleted = 0;
                int totalVmPorts = vm.getPorts().size();
                for (VMPort vmport : vm.getPorts()) {
                    if (vmport.getNetwork() == null) {
                        OSCEntityManager.delete(em, vmport);
                        portsDeleted++;
                    }
                }
                if (portsDeleted == totalVmPorts) {
                    // Only if all ports were deleted, delete the VM.
                    OSCEntityManager.delete(em, vm);
                }
            } else {
                vm.getSecurityGroupMembers().remove(this.sgm);
            }
            this.log.info("Deleting Security Group member from " + this.sgm.getSecurityGroup().getName());

        } else if (this.sgm.getType() == SecurityGroupMemberType.NETWORK) {
            Network network = this.sgm.getNetwork();

            if (network.getSecurityGroupMembers().size() == 1) {
                // Network has reference only to this SGM. Delete the network too.
                this.log.info("No other references to Network found. Deleting Network " + network.getName());
                for (VMPort vmPort : network.getPorts()) {
                    // If the VM was created on behalf of this port, delete it.
                    VM vm = vmPort.getVm();
                    OSCEntityManager.delete(em, vmPort);
                    if (vm != null) {
                        vm.removePort(vmPort);
                        if (vm.getSecurityGroupMembers().size() == 0 && vm.getPorts().size() <= 0) {
                            OSCEntityManager.delete(em, vm);
                        }
                    }
                }
                OSCEntityManager.delete(em, network);
            }
            this.log.info("Deleting Security Group member from " + this.sgm.getSecurityGroup().getName());
            network.getSecurityGroupMembers().remove(this.sgm);
        } else if (this.sgm.getType() == SecurityGroupMemberType.SUBNET) {
            Subnet subnet = this.sgm.getSubnet();

            if (subnet.getSecurityGroupMembers().size() == 1) {
                // Subnet has reference only to this SGM. Delete the subnet too.
                this.log.info("No other references to Subnet found. Deleting Subnet " + subnet.getName());
                for (VMPort vmPort : subnet.getPorts()) {
                    // If the VM was created on behalf of this port, delete it.
                    VM vm = vmPort.getVm();
                    OSCEntityManager.delete(em, vmPort);
                    if (vm != null) {
                        vm.removePort(vmPort);
                        if (vm.getSecurityGroupMembers().size() == 0 && vm.getPorts().size() <= 0) {
                            OSCEntityManager.delete(em, vm);
                        }
                    }
                }
                OSCEntityManager.delete(em, subnet);
            }
            this.log.info("Deleting Security Group member from " + this.sgm.getSecurityGroup().getName());
            subnet.getSecurityGroupMembers().remove(this.sgm);
        }
        OSCEntityManager.delete(em, this.sgm);
    }

    @Override
    public String getName() {
        return String.format("Deleting %s Security Group Member '%s' from Security Group '%s'", this.sgm.getType(),
                this.sgm.getMemberName(), this.sgm.getSecurityGroup().getName());
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.sgm.getSecurityGroup());
    }

}
