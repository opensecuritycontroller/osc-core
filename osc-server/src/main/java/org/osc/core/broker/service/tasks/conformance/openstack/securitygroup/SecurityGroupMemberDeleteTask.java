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

import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMember;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMemberType;
import org.osc.core.broker.model.entities.virtualization.k8s.Label;
import org.osc.core.broker.model.entities.virtualization.k8s.Pod;
import org.osc.core.broker.model.entities.virtualization.k8s.PodPort;
import org.osc.core.broker.model.entities.virtualization.openstack.Network;
import org.osc.core.broker.model.entities.virtualization.openstack.Subnet;
import org.osc.core.broker.model.entities.virtualization.openstack.VM;
import org.osc.core.broker.model.entities.virtualization.openstack.VMPort;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service=SecurityGroupMemberDeleteTask.class)
public class SecurityGroupMemberDeleteTask extends TransactionalTask {

    private final Logger log = LoggerFactory.getLogger(SecurityGroupMemberDeleteTask.class);

    private SecurityGroupMember sgm;

    public SecurityGroupMemberDeleteTask create(SecurityGroupMember sgm) {
        SecurityGroupMemberDeleteTask task = new SecurityGroupMemberDeleteTask();
        task.sgm = sgm;
        task.dbConnectionManager = this.dbConnectionManager;
        task.txBroadcastUtil = this.txBroadcastUtil;

        return task;
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
                        deleteFromDB(em, vmport);
                        portsDeleted++;
                    }
                }

                if (portsDeleted == totalVmPorts) {
                    // Only if all ports were deleted, delete the VM.
                    deleteFromDB(em, vm);
                }
            } else {
                vm.getSecurityGroupMembers().remove(this.sgm);
            }
            this.log.info("Deleting Security Group member from " + this.sgm.getSecurityGroup().getName());

        } else if (this.sgm.getType() == SecurityGroupMemberType.LABEL) {
            Label label = this.sgm.getLabel();

            if (label.getSecurityGroupMembers().size() == 1) {
                this.log.info("No other references to Label found. Deleting Label " + label.getValue());
                for (Pod pod : label.getPods()) {
                    if (pod.getLabels().size() == 1) {
                        for (PodPort podPort : pod.getPorts()) {
                            for (DistributedApplianceInstance dai : podPort.getDais()) {
                                dai.removeProtectedPort(podPort);
                                OSCEntityManager.update(em, dai, this.txBroadcastUtil);
                            }
                            OSCEntityManager.delete(em, podPort, this.txBroadcastUtil);
                        }
                        OSCEntityManager.delete(em, pod, this.txBroadcastUtil);
                    }
                }
                OSCEntityManager.delete(em, label, this.txBroadcastUtil);
            } else {
                label.getSecurityGroupMembers().remove(this.sgm);
            }
            this.log.info("Deleting Security Group member from " + this.sgm.getSecurityGroup().getName());

        }  else if (this.sgm.getType() == SecurityGroupMemberType.NETWORK) {
            Network network = this.sgm.getNetwork();

            if (network.getSecurityGroupMembers().size() == 1) {
                // Network has reference only to this SGM. Delete the network too.
                this.log.info("No other references to Network found. Deleting Network " + network.getName());
                deleteFromDB(em, network);
            }
            this.log.info("Deleting Security Group member from " + this.sgm.getSecurityGroup().getName());
            network.getSecurityGroupMembers().remove(this.sgm);
        } else if (this.sgm.getType() == SecurityGroupMemberType.SUBNET) {
            Subnet subnet = this.sgm.getSubnet();

            if (subnet.getSecurityGroupMembers().size() == 1) {
                // Subnet has reference only to this SGM. Delete the subnet too.
                this.log.info("No other references to Subnet found. Deleting Subnet " + subnet.getName());
                deleteFromDB(em, subnet);
            }
            this.log.info("Deleting Security Group member from " + this.sgm.getSecurityGroup().getName());
            subnet.getSecurityGroupMembers().remove(this.sgm);
        }
        OSCEntityManager.delete(em, this.sgm, this.txBroadcastUtil);
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

    private void deleteFromDB(EntityManager em , VMPort port) {
        this.log.info("Removing port " + port);
        VM vm = port.getVm();
        if (vm != null) {
            vm.removePort(port);
            port.setVm(null);
        }

        Network network = port.getNetwork();
        if (network != null) {
            network.removePort(port);
        }

        for (DistributedApplianceInstance dai : port.getDais()) {
            dai.removeProtectedPort(port);
            port.removeDai(dai);
        }

        OSCEntityManager.delete(em, port, this.txBroadcastUtil);
    }

    /**
     * This method is only called when all the ports are gone.
     * @param em
     * @param vm
     */
    private void deleteFromDB(EntityManager em, VM vm) {
        this.log.info("Removing vm " + vm);
        OSCEntityManager.delete(em, vm, this.txBroadcastUtil);
    }

    private void deleteFromDB(EntityManager em, Network network) {
        Set<VMPort> ports = new HashSet<>(network.getPorts());
        for (VMPort vmPort : ports) {
            // If the VM was created on behalf of this port, delete it.
            VM vm = vmPort.getVm();
            deleteFromDB(em, vmPort);
            if (vm != null) {
                if (vm.getSecurityGroupMembers().size() == 0 && vm.getPorts().size() <= 0) {
                    deleteFromDB(em, vm);
                }
            }
        }
        OSCEntityManager.delete(em, network, this.txBroadcastUtil);
    }

    private void deleteFromDB(EntityManager em, Subnet subnet) {
        Set<VMPort> ports = new HashSet<>(subnet.getPorts());
        for (VMPort vmPort : ports) {
            // If the VM was created on behalf of this port, delete it.
            VM vm = vmPort.getVm();
            deleteFromDB(em, vmPort);
            if (vm != null) {
                if (vm.getSecurityGroupMembers().size() == 0 && vm.getPorts().size() <= 0) {
                    deleteFromDB(em, vm);
                }
            }
        }
        OSCEntityManager.delete(em, subnet, this.txBroadcastUtil);
    }
}
