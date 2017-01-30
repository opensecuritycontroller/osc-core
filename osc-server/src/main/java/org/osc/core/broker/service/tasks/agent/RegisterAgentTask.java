package org.osc.core.broker.service.tasks.agent;

import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.rest.server.AgentAuthFilter;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osc.core.rest.client.agent.api.VmidcAgentApi;

import com.mcafee.vmidc.server.Server;

class RegisterAgentTask extends TransactionalTask {
    private static final Logger log = Logger.getLogger(RegisterAgentTask.class);
    private DistributedApplianceInstance dai;

    public RegisterAgentTask(DistributedApplianceInstance dai) {
        this.dai = dai;
    }

    @Override
    public String getName() {
        return "Register " + Server.PRODUCT_NAME + " Agent (" + this.dai.getName() + ")";
    }

    @Override
    public void executeTransaction(Session session) throws Exception {

        VmidcAgentApi agentApi = new VmidcAgentApi(this.dai.getIpAddress(), 8090, AgentAuthFilter.VMIDC_AGENT_LOGIN,
                AgentAuthFilter.VMIDC_AGENT_PASS);
        String response = agentApi.register();
        log.info("Register " + this.dai.getName() + " request sent. Response: " + response);
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.dai);
    }

}
