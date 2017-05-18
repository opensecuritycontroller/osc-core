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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.persistence.EntityManager;

import org.apache.log4j.Logger;
import org.jclouds.openstack.neutron.v2.domain.IP;
import org.jclouds.openstack.neutron.v2.domain.Port;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.job.TaskGuard;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMember;
import org.osc.core.broker.model.entities.virtualization.openstack.Subnet;
import org.osc.core.broker.model.entities.virtualization.openstack.VMPort;
import org.osc.core.broker.rest.client.openstack.jcloud.Endpoint;
import org.osc.core.broker.rest.client.openstack.jcloud.JCloudNeutron;
import org.osc.core.broker.service.persistence.DistributedApplianceInstanceEntityMgr;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.persistence.VMPortEntityManager;
import org.osc.core.broker.service.tasks.FailedWithObjectInfoTask;
import org.osc.core.broker.service.tasks.TransactionalMetaTask;
import org.osc.core.broker.service.tasks.conformance.openstack.deploymentspec.OpenstackUtil;

class SecurityGroupMemberSubnetUpdateTask extends TransactionalMetaTask {

    private final Logger log = Logger.getLogger(SecurityGroupMemberSubnetUpdateTask.class);
    private TaskGraph tg;

    private SecurityGroupMember sgm;
    private final String subnetName;

    public SecurityGroupMemberSubnetUpdateTask(SecurityGroupMember sgm, String subnetName) {
        this.sgm = sgm;
        this.subnetName = subnetName;
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {
        this.tg = new TaskGraph();
        this.sgm = em.find(SecurityGroupMember.class, this.sgm.getId());

        Subnet subnet = this.sgm.getSubnet();

        subnet.setName(this.subnetName);

        SecurityGroup sg = this.sgm.getSecurityGroup();

        JCloudNeutron neutron = null;

        try {
            neutron = new JCloudNeutron(new Endpoint(sg.getVirtualizationConnector(), sg.getTenantName()));

            List<Port> osPorts = neutron.listPortsBySubnet(subnet.getRegion(), sg.getTenantId(), subnet.getNetworkId(),
                    subnet.getOpenstackId(), subnet.isProtectExternal());

            List<String> existingOsPortIds = new ArrayList<>();

            for (Port osPort : osPorts) {
                existingOsPortIds.add(osPort.getId());
                // Check to see if the port belongs to one of our DAI. Only if the port does not belong to the DAI
                // Add it to our DB for it to be protected later, so we prevent self inspection loop
                DistributedApplianceInstance daiForPort = DistributedApplianceInstanceEntityMgr.getByOSServerId(
                        em, osPort.getDeviceId());
                if (daiForPort == null) {
                    VMPort vmPort = VMPortEntityManager.findByOpenstackId(em, osPort.getId());
                    if (vmPort == null) {
                        // get list of IP address from port
                        List<String> ipAddresses = new ArrayList<>();
                        for (IP ip : osPort.getFixedIps()) {
                            ipAddresses.add(ip.getIpAddress());
                        }
                        vmPort = new VMPort(subnet, osPort.getMacAddress(), subnet.getOpenstackId(), osPort.getId(),
                                ipAddresses);
                        OSCEntityManager.create(em, vmPort);
                        this.log.info("Creating port for Subnet '" + subnet.getName() + "' with Port:" + vmPort);
                    } else {
                        //Port exists check if it belongs to a VM
                        if (vmPort.getVm() != null) {
                            Iterator<SecurityGroupMember> iterator = vmPort.getVm().getSecurityGroupMembers()
                                    .iterator();
                            if (iterator.hasNext()) {
                                SecurityGroup otherSecurityGroup = iterator.next().getSecurityGroup();
                                if (!otherSecurityGroup.equals(sg)) {
                                    String errMessage = String
                                            .format("VM Port with MAC '%s' (VM '%s') belonging to subnet member '%s' is already being protected by Security Group '%s'",
                                                    vmPort.getMacAddresses(), vmPort.getVm().getName(), subnet.getName(),
                                                    otherSecurityGroup.getName());
                                    this.tg.addTask(new FailedWithObjectInfoTask(String.format(
                                            "Validating port with mac '%s' information", vmPort.getMacAddresses()),
                                            errMessage, LockObjectReference.getObjectReferences(sg)));
                                }
                            }
                        }
                        // Port exists check if it belongs to a Network
                        if (vmPort.getNetwork() != null) {
                            Iterator<SecurityGroupMember> iterator = vmPort.getNetwork().getSecurityGroupMembers()
                                    .iterator();
                            if (iterator.hasNext()) {
                                SecurityGroup otherSecurityGroup = iterator.next().getSecurityGroup();
                                if (!otherSecurityGroup.equals(sg)) {
                                    String errMessage = String
                                            .format("VM Port with MAC '%s' (Network '%s') belonging to subnet member '%s' is already being protected by Security Group '%s'",
                                                    vmPort.getMacAddresses(), vmPort.getNetwork().getName(),
                                                    subnet.getName(), otherSecurityGroup.getName());
                                    this.tg.addTask(new FailedWithObjectInfoTask(String.format(
                                            "Validating port with mac '%s' information", vmPort.getMacAddresses()),
                                            errMessage, LockObjectReference.getObjectReferences(sg)));
                                }
                            }
                        }
                        // Port belongs to this Subnet. It it was mark deleted by Protect External flag and user reverted it then revert mark deletion flag and remove it from staled port list
                        if (vmPort.getMarkedForDeletion()) {
                            OSCEntityManager.unMarkDeleted(em, vmPort);
                            existingOsPortIds.remove(osPort.getId());
                        }
                    }
                    if (!subnet.isProtectExternal()) {
                        OpenstackUtil.discoverVmForPort(em, subnet.getRegion(), sg, osPort, vmPort);
                    }
                }

            }
            // Any ports not listed from openstack but are in our database are stale and need to be removed(after hooks
            // are removed) so marking them as deleted
            this.tg.appendTask(new MarkStalePortsAsDeletedTask(subnet, existingOsPortIds),
                    TaskGuard.ALL_PREDECESSORS_COMPLETED);

        } finally {
            if (neutron != null) {
                neutron.close();
            }
        }

        OSCEntityManager.update(em, subnet);
    }

    @Override
    public String getName() {
        return String.format("Updating Security Group Member Subnet '%s'", this.subnetName);
    }

    @Override
    public TaskGraph getTaskGraph() {
        return this.tg;
    }

}
