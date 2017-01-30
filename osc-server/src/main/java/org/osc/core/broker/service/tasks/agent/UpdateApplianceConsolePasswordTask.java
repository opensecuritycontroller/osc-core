package org.osc.core.broker.service.tasks.agent;

import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.rest.server.AgentAuthFilter;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osc.core.rest.client.agent.api.VmidcAgentApi;

public class UpdateApplianceConsolePasswordTask extends TransactionalTask {

    final Logger log = Logger.getLogger(UpdateApplianceConsolePasswordTask.class);

    private DistributedApplianceInstance dai;
    private String newPassword;

    public UpdateApplianceConsolePasswordTask(DistributedApplianceInstance dai, String newPassword) {
        this.dai = dai;
        this.newPassword = newPassword;
        this.name = getName();
    }

    @Override
    public void executeTransaction(Session session) throws Exception {

        VmidcAgentApi agentApi = new VmidcAgentApi(dai.getIpAddress(), 8090, AgentAuthFilter.VMIDC_AGENT_LOGIN,
                AgentAuthFilter.VMIDC_AGENT_PASS);

        agentApi.updateApplianceConsolePassowrd(dai.getCurrentConsolePassword(), this.newPassword);

        // Indicating new password got updated successfully and thus made current
        dai.setCurrentConsolePassword(this.newPassword);
        // Reset new password pending field
        dai.setNewConsolePassword(null);
        EntityManager.update(session, dai);
    }

    @Override
    public String getName() {
        return "Propagate console password to '" + dai.getName() + "'";
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(dai);
    }

}
