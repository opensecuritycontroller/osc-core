package org.osc.core.broker.service.tasks.conformance.openstack.securitygroup;

import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.hibernate.Session;
import org.jboss.logging.Logger;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMember;
import org.osc.core.broker.model.entities.virtualization.openstack.VM;
import org.osc.core.broker.model.entities.virtualization.openstack.VMPort;
import org.osc.core.broker.rest.client.openstack.discovery.VmDiscoveryCache.PortInfo;
import org.osc.core.broker.rest.client.openstack.discovery.VmDiscoveryCache.VmInfo;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;

class SecurityGroupMemberVmUpdateTask extends TransactionalTask {

    private final Logger log = Logger.getLogger(SecurityGroupMemberVmUpdateTask.class);

    private SecurityGroupMember sgm;
    private VmInfo vmInfo;

    public SecurityGroupMemberVmUpdateTask(SecurityGroupMember sgm, VmInfo vmInfo) {
        this.sgm = sgm;
        this.vmInfo = vmInfo;
    }

    @Override
    public void executeTransaction(Session session) throws Exception {
        this.sgm = (SecurityGroupMember) session.get(SecurityGroupMember.class, this.sgm.getId());

        VM vm = this.sgm.getVm();

        // Verify VM info
        vm.setName(this.vmInfo.name);
        vm.setHost(this.vmInfo.host);
        EntityManager.update(session, vm);

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
                EntityManager.update(session, this.sgm);
                EntityManager.create(session, newVmPort);
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
                EntityManager.markDeleted(session, vmPort);
                this.log.info("Marking Deleting port for VM '" + vm.getName() + "' (" + vm.getOpenstackId()
                        + "). Port:" + vmPort);
            } else {
                // Verify existing port info
                vmPort.setOsNetworkId(portInfo.osNetworkId);
                vmPort.setOpenstackId(portInfo.osPortId);

                EntityManager.update(session, vmPort);
            }
        }
    }

    @Override
    public String getName() {
        return String.format("Updating Security Group Member VM '%s'", this.vmInfo.name);
    }

}
