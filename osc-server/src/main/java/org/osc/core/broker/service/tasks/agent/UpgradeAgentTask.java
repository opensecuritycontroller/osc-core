package org.osc.core.broker.service.tasks.agent;

import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.rest.server.AgentAuthFilter;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osc.core.rest.client.agent.api.VmidcAgentApi;
import org.osc.core.util.ServerUtil;

import com.mcafee.vmidc.server.Server;

public class UpgradeAgentTask extends TransactionalTask {

    private static final Logger log = Logger.getLogger(UpgradeAgentTask.class);
    private DistributedApplianceInstance dai;

    public UpgradeAgentTask(DistributedApplianceInstance dai) {
        this.dai = dai;
    }

    @Override
    public String getName() {
        return "Upgrading " + Server.PRODUCT_NAME + " Agent (" + this.dai.getName() + ")";
    }

    @Override
    public void executeTransaction(Session session) throws Exception {
        VmidcAgentApi agentApi = new VmidcAgentApi(this.dai.getIpAddress(), 8090, AgentAuthFilter.VMIDC_AGENT_LOGIN,
                AgentAuthFilter.VMIDC_AGENT_PASS);
        agentApi.upgrade("https://" + ServerUtil.getServerIP() + ":" + Server.getApiPort().toString()
                + "/ovf/agentUpgradeBundle.zip");
        log.info("Agent Upgrade request sent to DAI " + this.dai.getName());
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.dai);
    }

}
