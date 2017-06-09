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

import java.util.List;

import javax.persistence.EntityManager;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.log4j.Logger;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMember;
import org.osc.core.broker.model.entities.virtualization.openstack.VM;
import org.osc.core.broker.model.entities.virtualization.openstack.VMPort;
import org.osc.core.broker.rest.client.openstack.discovery.VmDiscoveryCache.PortInfo;
import org.osc.core.broker.rest.client.openstack.discovery.VmDiscoveryCache.VmInfo;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osgi.service.component.annotations.Component;

@Component(service=SecurityGroupMemberVmUpdateTask.class)
public class SecurityGroupMemberVmUpdateTask extends TransactionalTask {

    private final Logger log = Logger.getLogger(SecurityGroupMemberVmUpdateTask.class);

    private SecurityGroupMember sgm;
    private VmInfo vmInfo;

    public SecurityGroupMemberVmUpdateTask create(SecurityGroupMember sgm, VmInfo vmInfo) {
        SecurityGroupMemberVmUpdateTask task = new SecurityGroupMemberVmUpdateTask();
        task.sgm = sgm;
        task.vmInfo = vmInfo;
        task.dbConnectionManager = this.dbConnectionManager;
        task.txBroadcastUtil = this.txBroadcastUtil;

        return task;
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {
        this.sgm = em.find(SecurityGroupMember.class, this.sgm.getId());

        VM vm = this.sgm.getVm();

        // Verify VM info
        vm.setName(this.vmInfo.name);
        vm.setHost(this.vmInfo.host);
        OSCEntityManager.update(em, vm, this.txBroadcastUtil);

        // Verify ports info
        for (PortInfo portInfo : this.vmInfo.macAddressToPortMap.values()) {
            boolean found = false;
            for (VMPort vmPort : vm.getPorts()) {
                List<String> macList = vmPort.getMacAddresses();
                if (CollectionUtils.isNotEmpty(macList)){
                    if (portInfo.macAddress.equals(macList.get(0))) {
                        found = true;
                        break;
                    }
                }
            }
            if (!found) {
                // Create new missing port
                VMPort newVmPort = new VMPort(vm, portInfo.macAddress, portInfo.osNetworkId, portInfo.osPortId,
                        portInfo.getPortIPs());
                OSCEntityManager.update(em, this.sgm, this.txBroadcastUtil);
                OSCEntityManager.create(em, newVmPort, this.txBroadcastUtil);
                this.log.info("Creating port for VM '" + vm.getName() + "' (" + vm.getOpenstackId() + "). Port:"
                        + newVmPort);
            }
        }
        for (VMPort vmPort : vm.getPorts()) {
            PortInfo portInfo = null;
            List<String> macAddresses = vmPort.getMacAddresses();
            if (CollectionUtils.isNotEmpty(macAddresses)){
                portInfo = this.vmInfo.macAddressToPortMap.get(vmPort.getMacAddresses().get(0));
            }
            if (portInfo == null) {
                OSCEntityManager.markDeleted(em, vmPort, this.txBroadcastUtil);
                this.log.info("Marking Deleting port for VM '" + vm.getName() + "' (" + vm.getOpenstackId()
                        + "). Port:" + vmPort);
            } else {
                // Verify existing port info
                vmPort.setOsNetworkId(portInfo.osNetworkId);
                vmPort.setOpenstackId(portInfo.osPortId);

                OSCEntityManager.update(em, vmPort, this.txBroadcastUtil);
            }
        }
    }

    @Override
    public String getName() {
        return String.format("Updating Security Group Member VM '%s'", this.vmInfo.name);
    }

}
