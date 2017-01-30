package org.osc.core.broker.service.tasks.agent;

import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.rest.server.AgentAuthFilter;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osc.core.broker.util.db.HibernateUtil;
import org.osc.core.rest.client.agent.api.VmidcAgentApi;

class AgentInterfaceEndpointMapRemoveTask extends TransactionalTask {

    final Logger log = Logger.getLogger(AgentInterfaceEndpointMapRemoveTask.class);

    private DistributedApplianceInstance dai;
    private String interfaceTag;

    public AgentInterfaceEndpointMapRemoveTask(DistributedApplianceInstance dai, String interfaceTag) {
        this.dai = dai;
        this.interfaceTag = interfaceTag;
        this.name = getName();
    }

    @Override
    public void executeTransaction(Session session) throws Exception {
        this.dai = (DistributedApplianceInstance) session.get(DistributedApplianceInstance.class, this.dai.getId());

        try {
            VmidcAgentApi agentApi = new VmidcAgentApi(this.dai.getIpAddress(), 8090, AgentAuthFilter.VMIDC_AGENT_LOGIN,
                    AgentAuthFilter.VMIDC_AGENT_PASS);
            agentApi.updateInterfaceEndpointMap(this.interfaceTag, null);

        } catch (Exception e) {
            this.log.warn("Fail to remove Security Group Interface '" + this.interfaceTag + "' mapping for DAI '"
                    + this.dai.getName() + "'");

            markDaiPolicyUpdateFailed();

            throw e;
        }
    }

    @Override
    public String getName() {
        return "Remove Traffic Policy Mapping '" + this.interfaceTag + "' for DAI '" + this.dai.getName() + "'";
    }

    private void markDaiPolicyUpdateFailed() {
        Session session = HibernateUtil.getSessionFactory().openSession();

        try {
            Transaction tx = session.beginTransaction();

            // Setting dirty flag for next successful DAI registration, at which time, we'll sync them all
            this.dai.setPolicyMapOutOfSync(true);
            EntityManager.update(session, this.dai);

            tx.commit();
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.dai);
    }

}
