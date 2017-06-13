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

import javax.persistence.EntityManager;

import org.jboss.logging.Logger;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMember;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMemberType;
import org.osc.core.broker.model.entities.virtualization.openstack.Network;
import org.osc.core.broker.model.entities.virtualization.openstack.Subnet;
import org.osc.core.broker.model.entities.virtualization.openstack.VM;
import org.osc.core.broker.model.entities.virtualization.openstack.VMPort;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;

class VmPortDeleteFromDbTask extends TransactionalTask {

    private final Logger log = Logger.getLogger(VmPortDeleteFromDbTask.class);

    private final SecurityGroupMember sgm;
    private VMPort vmPort;

    public VmPortDeleteFromDbTask(SecurityGroupMember sgm, VMPort vmPort) {
        this.sgm = sgm;
        this.vmPort = vmPort;
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {
        this.vmPort = em.find(VMPort.class, this.vmPort.getId());

        if (this.sgm.getType() == SecurityGroupMemberType.VM) {
            VM vm = this.sgm.getVm();
            this.log.info(String.format("Deleting Security Group Member VM '%s' Port with mac '%s'", vm.getName(),
                    this.vmPort.getMacAddresses()));
            vm.removePort(this.vmPort);
            OSCEntityManager.delete(em, this.vmPort);
        } else if (this.sgm.getType() == SecurityGroupMemberType.NETWORK) {
            Network network = this.sgm.getNetwork();
            this.log.info(String.format("Deleting VM Port with MAC '%s' of Network '%s' from Security Group '%s'",
                    this.vmPort.getMacAddresses(), network.getName(), this.sgm.getSecurityGroup().getName()));
            network.removePort(this.vmPort);

            VM vm = this.vmPort.getVm();
            // If the VM was created on behalf of this port and has no other ports, delete it.
            if (vm != null && vm.getSecurityGroupMembers().size() == 0 && vm.getPorts().size() <= 1) {
                OSCEntityManager.delete(em, vm);
            }
            OSCEntityManager.delete(em, this.vmPort);
        } else if (this.sgm.getType() == SecurityGroupMemberType.SUBNET) {
            Subnet subnet = this.sgm.getSubnet();
            this.log.info(String.format("Deleting VM Port with MAC '%s' of Subnet '%s' from Security Group '%s'",
                    this.vmPort.getMacAddresses(), subnet.getName(), this.sgm.getSecurityGroup().getName()));
            subnet.removePort(this.vmPort);

            if (!subnet.isProtectExternal()) {
                VM vm = this.vmPort.getVm();
                // If the VM was created on behalf of this port and has no other ports, delete it.
                if (vm != null && vm.getSecurityGroupMembers().size() == 0 && vm.getPorts().size() <= 1) {
                    OSCEntityManager.delete(em, vm);
                }
            }
            OSCEntityManager.delete(em, this.vmPort);
        }

    }

    @Override
    public String getName() {
        if (this.sgm.getType() == SecurityGroupMemberType.VM && this.sgm.getVm() != null) {
            return String.format("Deleting Security Group Member VM '%s' Port with mac '%s'", this.sgm.getVm()
                    .getName(), this.vmPort.getMacAddresses());
        } else if (this.sgm.getType() == SecurityGroupMemberType.NETWORK && this.sgm.getNetwork() != null) {
            return String
                    .format("Deleting VM Port with MAC '%s' of Network '%s' from Security Group '%s'", this.vmPort
                            .getMacAddresses(), this.sgm.getNetwork().getName(), this.sgm.getSecurityGroup().getName());
        } else if (this.sgm.getType() == SecurityGroupMemberType.SUBNET && this.sgm.getSubnet() != null) {
            return String.format("Deleting VM Port with MAC '%s' of Subnet '%s' from Security Group '%s'",
                    this.vmPort.getMacAddresses(), this.sgm.getSubnet().getName(), this.sgm.getSecurityGroup().getName());
        }
        // We should never get here
        throw new IllegalStateException(
                "Vm port delete needs to specify either the network or vm on behalf of which its running");
    }

}
