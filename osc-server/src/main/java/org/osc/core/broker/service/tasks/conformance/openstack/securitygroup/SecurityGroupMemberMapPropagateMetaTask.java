package org.osc.core.broker.service.tasks.conformance.openstack.securitygroup;

import java.util.Set;

import org.hibernate.Session;
import org.osc.core.broker.job.TaskGraph;
import org.osc.core.broker.job.TaskGuard;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupInterface;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMember;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMemberType;
import org.osc.core.broker.model.entities.virtualization.openstack.VMPort;
import org.osc.core.broker.model.plugin.manager.ManagerApiFactory;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.tasks.TransactionalMetaTask;
import org.osc.core.broker.service.tasks.agent.AgentsInterfaceEndpointMapUpdateMetaTask;
import org.osc.core.broker.service.tasks.conformance.securitygroup.MgrSecurityGroupCheckMetaTask;
import org.osc.core.rest.client.agent.model.input.EndpointGroupList;
import org.osc.sdk.manager.api.ApplianceManagerApi;

public class SecurityGroupMemberMapPropagateMetaTask extends TransactionalMetaTask {

    private SecurityGroup sg;

    private TaskGraph tg;

    public SecurityGroupMemberMapPropagateMetaTask(SecurityGroup sg) {
        this.sg = sg;
    }

    @Override
    public void executeTransaction(Session session) throws Exception {
        this.sg = (SecurityGroup) session.get(SecurityGroup.class, this.sg.getId());

        this.tg = new TaskGraph();
        for (SecurityGroupInterface sgi : this.sg.getSecurityGroupInterfaces()) {
            VirtualSystem vs = sgi.getVirtualSystem();
            ApplianceManagerApi managerApi = ManagerApiFactory.createApplianceManagerApi(vs);
            if (vs.getMgrId() != null  && managerApi.isSecurityGroupSyncSupport()) {
                if (managerApi.isAgentManaged() && !sgi.getMarkedForDeletion()) {
                    // Sync SG members mapping to all DAIs
                    this.tg.addTask(
                            new AgentsInterfaceEndpointMapUpdateMetaTask(vs, sgi.getTag(), getEndpointGroupList()));
                }
                // Sync SG members mapping to the manager directly
                this.tg.addTask(new MgrSecurityGroupCheckMetaTask(vs, this.sg), TaskGuard.ALL_PREDECESSORS_COMPLETED);
            }
        }
    }

    private EndpointGroupList getEndpointGroupList() throws VmidcBrokerValidationException {
        EndpointGroupList.EndpointGroup epgMacs = new EndpointGroupList.EndpointGroup();
        epgMacs.id = this.sg.getId().toString();
        epgMacs.name = this.sg.getName();
        epgMacs.type = SecurityGroupMemberType.MAC.toString();

        EndpointGroupList.EndpointGroup epgIpAddresses = new EndpointGroupList.EndpointGroup();
        epgIpAddresses.id = this.sg.getId().toString();
        epgIpAddresses.name = this.sg.getName();
        epgIpAddresses.type = SecurityGroupMemberType.IP.toString();

        for (SecurityGroupMember sgm : this.sg.getSecurityGroupMembers()) {
            for (VMPort port : sgm.getPorts()) {
                epgMacs.addresses.addAll(port.getMacAddresses());
                for (String ipAddress : port.getPortIPs()) {
                    epgIpAddresses.addresses.add(ipAddress);
                }
            }
        }

        EndpointGroupList epgl = new EndpointGroupList();
        epgl.endpointGroups.add(epgMacs);
        epgl.endpointGroups.add(epgIpAddresses);
        return epgl;
    }

    @Override
    public String getName() {
        return String.format("Checking to propogate Security Group member list to appliances/managers '%s'", this.sg.getName());
    }

    @Override
    public TaskGraph getTaskGraph() {
        return this.tg;
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.sg);
    }

}
