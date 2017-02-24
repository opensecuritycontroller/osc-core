package org.osc.core.broker.service.tasks.conformance.openstack.securitygroup;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.hibernate.Session;
import org.jboss.logging.Logger;
import org.jclouds.openstack.neutron.v2.domain.IP;
import org.jclouds.openstack.neutron.v2.domain.Port;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.job.TaskGuard;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMember;
import org.osc.core.broker.model.entities.virtualization.openstack.Network;
import org.osc.core.broker.model.entities.virtualization.openstack.VMPort;
import org.osc.core.broker.rest.client.openstack.jcloud.Endpoint;
import org.osc.core.broker.rest.client.openstack.jcloud.JCloudNeutron;
import org.osc.core.broker.service.persistence.DistributedApplianceInstanceEntityMgr;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.persistence.VMPortEntityManager;
import org.osc.core.broker.service.tasks.FailedWithObjectInfoTask;
import org.osc.core.broker.service.tasks.TransactionalMetaTask;
import org.osc.core.broker.service.tasks.conformance.openstack.deploymentspec.OpenstackUtil;

class SecurityGroupMemberNetworkUpdateTask extends TransactionalMetaTask {

    private final Logger log = Logger.getLogger(SecurityGroupMemberNetworkUpdateTask.class);
    private TaskGraph tg;

    private SecurityGroupMember sgm;
    private final String networkName;

    public SecurityGroupMemberNetworkUpdateTask(SecurityGroupMember sgm, String networkName) {
        this.sgm = sgm;
        this.networkName = networkName;
    }

    @Override
    public void executeTransaction(Session session) throws Exception {
        this.tg = new TaskGraph();
        this.sgm = (SecurityGroupMember) session.get(SecurityGroupMember.class, this.sgm.getId());

        Network network = this.sgm.getNetwork();

        network.setName(this.networkName);

        SecurityGroup sg = this.sgm.getSecurityGroup();

        JCloudNeutron neutron = null;

        try {
            neutron = new JCloudNeutron(new Endpoint(sg.getVirtualizationConnector(), sg.getTenantName()));

            List<Port> osPorts = neutron.listComputePortsByNetwork(network.getRegion(), sg.getTenantId(),
                    network.getOpenstackId());
            List<String> existingOsPortIds = new ArrayList<>();
            for (Port osPort : osPorts) {
                existingOsPortIds.add(osPort.getId());
                // Check to see if the port belongs to one of our DAI. Only if the port does not belong to the DAI
                // Add it to our DB for it to be protected later, so we prevent self inspection loop
                DistributedApplianceInstance daiForPort = DistributedApplianceInstanceEntityMgr.getByOSServerId(
                        session, osPort.getDeviceId());
                if (daiForPort == null) {
                    VMPort vmPort = VMPortEntityManager.findByOpenstackId(session, osPort.getId());
                    if (vmPort == null) {
                        // get list of IP address from port
                        List<String> ipAddresses = new ArrayList<>();
                        for (IP ip : osPort.getFixedIps()) {
                            ipAddresses.add(ip.getIpAddress());
                        }
                        vmPort = new VMPort(network, osPort.getMacAddress(), network.getOpenstackId(), osPort.getId(),
                                ipAddresses);
                        EntityManager.create(session, vmPort);
                        this.log.info("Creating port for Network '" + network.getName() + "' with Port:" + vmPort);
                    } else {
                        //Port exists check if it belongs to a VM
                        if (vmPort.getVm() != null) {
                            Iterator<SecurityGroupMember> iterator = vmPort.getVm().getSecurityGroupMembers()
                                    .iterator();
                            if (iterator.hasNext()) {
                                SecurityGroup otherSecurityGroup = iterator.next().getSecurityGroup();
                                if (!otherSecurityGroup.equals(sg)) {
                                    String errMessage = String
                                            .format("VM Port with MAC '%s' (VM '%s') belonging to network member '%s' is already being protected by Security Group '%s'",
                                                    vmPort.getMacAddresses(), vmPort.getVm().getName(),
                                                    network.getName(), otherSecurityGroup.getName());
                                    this.tg.addTask(new FailedWithObjectInfoTask(String.format(
                                            "Validating port with mac '%s' information", vmPort.getMacAddresses()),
                                            errMessage, LockObjectReference.getObjectReferences(sg)));
                                }
                            }
                        }
                        // Port exists check if it belongs to a Subnet
                        if (vmPort.getSubnet() != null) {
                            Iterator<SecurityGroupMember> iterator = vmPort.getSubnet().getSecurityGroupMembers()
                                    .iterator();
                            if (iterator.hasNext()) {
                                SecurityGroup otherSecurityGroup = iterator.next().getSecurityGroup();
                                if (!otherSecurityGroup.equals(sg)) {
                                    String errMessage = String
                                            .format("VM Port with MAC '%s' (Subnet '%s') belonging to network member '%s' is already being protected by Security Group '%s'",
                                                    vmPort.getMacAddresses(), vmPort.getSubnet().getName(),
                                                    network.getName(), otherSecurityGroup.getName());
                                    this.tg.addTask(new FailedWithObjectInfoTask(String.format(
                                            "Validating port with mac '%s' information", vmPort.getMacAddresses()),
                                            errMessage, LockObjectReference.getObjectReferences(sg)));
                                }
                            }
                        }
                        // Port belongs to this network too.
                    }
                    OpenstackUtil.discoverVmForPort(session, network.getRegion(), sg, osPort, vmPort);
                }
            }
            // Any ports not listed from openstack but are in our database are stale and need to be removed(after hooks
            // are removed) so marking them as deleted
            this.tg.appendTask(new MarkStalePortsAsDeletedTask(network, existingOsPortIds),
                    TaskGuard.ALL_PREDECESSORS_COMPLETED);

        } finally {
            if (neutron != null) {
                neutron.close();
            }
        }

        EntityManager.update(session, network);
    }

    @Override
    public String getName() {
        return String.format("Updating Security Group Member Network '%s'", this.networkName);
    }

    @Override
    public TaskGraph getTaskGraph() {
        return this.tg;
    }

}
