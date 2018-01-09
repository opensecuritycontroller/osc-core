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

import static java.util.stream.Collectors.toSet;

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

            vm.getSecurityGroupMembers().remove(this.sgm);
            if (vm.getSecurityGroupMembers().size() == 0) {
                // VM has reference only to this SGM. Delete the vm too.
                this.log.info("No other references to VM found. Deleting VM " + vm.getName());
                Set<VMPort> portsToRemove = vm.getPorts().stream().filter(p -> p.getNetwork() == null).collect(toSet());

                // If portsToRemove equals all ports, VM itself is deleted.
                deleteFromDB(em, portsToRemove);
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
                deleteFromDB(em, network.getPorts());
                OSCEntityManager.delete(em, network, this.txBroadcastUtil);
            }
            this.log.info("Deleting Security Group member from " + this.sgm.getSecurityGroup().getName());
            network.getSecurityGroupMembers().remove(this.sgm);
        } else if (this.sgm.getType() == SecurityGroupMemberType.SUBNET) {
            Subnet subnet = this.sgm.getSubnet();

            if (subnet.getSecurityGroupMembers().size() == 1) {
                // Subnet has reference only to this SGM. Delete the subnet too.
                this.log.info("No other references to Subnet found. Deleting Subnet " + subnet.getName());
                deleteFromDB(em, subnet.getPorts());
                OSCEntityManager.delete(em, subnet, this.txBroadcastUtil);
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

    private void deleteFromDB(EntityManager em, Set<VMPort> ports) {
        Set<VM> vmsToRemove = new HashSet<>();

        // guard against ConcurrentModificationException;
        Set<VMPort> portsClone = new HashSet<>(ports);
        for (VMPort vmPort : portsClone) {
            VM vm = vmPort.getVm();
            if (vm != null) {
                vm.removePort(vmPort);
                vmPort.setVm(null);
            }

            Network network = vmPort.getNetwork();
            if (network != null) {
                network.removePort(vmPort);
            }

            for (DistributedApplianceInstance dai : vmPort.getDais()) {
                dai.removeProtectedPort(vmPort);
                vmPort.removeDai(dai);
            }

            this.log.info("Removing port " + vmPort);
            OSCEntityManager.delete(em, vmPort, this.txBroadcastUtil);

            if (vm != null) {
                if (vm.getSecurityGroupMembers().size() == 0 && vm.getPorts().size() <= 0) {
                    vmsToRemove.add(vm);
                }
            }
        }

        for (VM vm : vmsToRemove) {
            this.log.info("Removing vm " + vm);
            OSCEntityManager.delete(em, vm, this.txBroadcastUtil);
        }
    }
}
