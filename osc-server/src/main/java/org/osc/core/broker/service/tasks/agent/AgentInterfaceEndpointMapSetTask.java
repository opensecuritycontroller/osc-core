package org.osc.core.broker.service.tasks.agent;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.virtualization.SecurityGroup;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupInterface;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMember;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMemberType;
import org.osc.core.broker.rest.client.nsx.model.ContainerSet;
import org.osc.core.broker.rest.client.nsx.model.ContainerSet.Container;
import org.osc.core.broker.rest.server.AgentAuthFilter;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osc.core.broker.util.db.HibernateUtil;
import org.osc.core.rest.client.agent.api.VmidcAgentApi;
import org.osc.core.rest.client.agent.model.input.dpaipc.InterfaceEndpointMap;

public class AgentInterfaceEndpointMapSetTask extends TransactionalTask {

    final Logger log = Logger.getLogger(AgentInterfaceEndpointMapSetTask.class);

    private DistributedApplianceInstance dai;

    public AgentInterfaceEndpointMapSetTask(DistributedApplianceInstance dai) {
        this.dai = dai;
        this.name = getName();
    }

    @Override
    public void executeTransaction(Session session) throws Exception {

        this.dai = (DistributedApplianceInstance) session.get(DistributedApplianceInstance.class, this.dai.getId());

        InterfaceEndpointMap interfaceEndpointMap = new InterfaceEndpointMap();
        for (SecurityGroupInterface sgi : this.dai.getVirtualSystem().getSecurityGroupInterfaces()) {
            ContainerSet containerSet = new ContainerSet();
            for (SecurityGroup sg : sgi.getSecurityGroups()) {
                List<String> ipAddress = new ArrayList<String>();
                for (SecurityGroupMember sgm : sg.getSecurityGroupMembers()) {
                    if (sgm.getType() == SecurityGroupMemberType.IP) {
                        ipAddress.add(sgm.getAddress());
                    }
                }
                if (!ipAddress.isEmpty()) {
                    Container container = new Container();
                    container.setId(sg.getNsxId());
                    container.setName(sg.getName());
                    container.setType(SecurityGroupMemberType.IP.toString());
                    container.setAddress(ipAddress);
                    containerSet.getList().add(container);
                }
                List<String> macAddress = new ArrayList<String>();
                for (SecurityGroupMember sgm : sg.getSecurityGroupMembers()) {
                    if (sgm.getType() == SecurityGroupMemberType.MAC) {
                        macAddress.add(sgm.getAddress());
                    }
                }
                if (!macAddress.isEmpty()) {
                    Container container = new Container();
                    container.setId(sg.getNsxId());
                    container.setName(sg.getName());
                    container.setType(SecurityGroupMemberType.MAC.toString());
                    container.setAddress(macAddress);
                    containerSet.getList().add(container);
                }
            }
            interfaceEndpointMap.updateInterfaceEndpointMap(sgi.getTag(), containerSet.toIscEndpointGroupSet());
        }

        try {
            VmidcAgentApi agentApi = new VmidcAgentApi(this.dai.getIpAddress(), 8090, AgentAuthFilter.VMIDC_AGENT_LOGIN,
                    AgentAuthFilter.VMIDC_AGENT_PASS);
            agentApi.setInterfaceEndpointMap(interfaceEndpointMap);

            // Avoid unnecessary changes
            if (this.dai.isPolicyMapOutOfSync()) {
                this.dai.setPolicyMapOutOfSync(false);
                EntityManager.update(session, this.dai);
            }

        } catch (Exception e) {
            this.log.warn("Fail to set Security Group Interfaces map of DAI '" + this.dai.getName() + "'");

            markDaiPolicyUpdateFailed();

            throw e;
        }

    }

    private void markDaiPolicyUpdateFailed() {
        Session session = HibernateUtil.getSessionFactory().openSession();

        try {
            Transaction tx = session.beginTransaction();

            DistributedApplianceInstance dai = (DistributedApplianceInstance) session
                    .get(DistributedApplianceInstance.class, this.dai.getId());

            // Setting dirty flag for next successful DAI registration, at which time, we'll sync them all
            dai.setPolicyMapOutOfSync(true);
            EntityManager.update(session, dai);

            tx.commit();
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    @Override
    public String getName() {
        return "Setting Traffic Policy Mapping for DAI '" + this.dai.getName() + "'";
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.dai);
    }

}
