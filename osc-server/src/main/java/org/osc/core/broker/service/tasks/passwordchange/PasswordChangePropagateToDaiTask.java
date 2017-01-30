package org.osc.core.broker.service.tasks.passwordchange;

import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.rest.server.AgentAuthFilter;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osc.core.broker.service.tasks.network.IpChangePropagateToDaiTask;
import org.osc.core.rest.client.agent.api.VmidcAgentApi;
import org.osc.core.util.EncryptionUtil;

import com.mcafee.vmidc.server.Server;



public class PasswordChangePropagateToDaiTask extends TransactionalTask {

    final Logger log = Logger.getLogger(IpChangePropagateToDaiTask.class);

    private DistributedApplianceInstance dai;

    public PasswordChangePropagateToDaiTask(DistributedApplianceInstance dai) {
        this.dai = dai;
    }

    @Override
    public void executeTransaction(Session session) throws Exception {

        this.dai = (DistributedApplianceInstance) session.get(DistributedApplianceInstance.class, this.dai.getId());

        VmidcAgentApi agentApi = new VmidcAgentApi(this.dai.getIpAddress(), 8090, AgentAuthFilter.VMIDC_AGENT_LOGIN,
                EncryptionUtil.decrypt(this.dai.getPassword()));

        agentApi.updateVmidcServerPassowrd(EncryptionUtil.encrypt(AgentAuthFilter.VMIDC_AGENT_PASS));

        // If we're successful changing agent password, persist it (if different).
        if (!this.dai.getPassword().equals(EncryptionUtil.encrypt(AgentAuthFilter.VMIDC_AGENT_PASS))) {
            this.dai.setPassword(EncryptionUtil.encrypt(AgentAuthFilter.VMIDC_AGENT_PASS));
            EntityManager.update(session, this.dai);
        }
    }

    @Override
    public String getName() {
        return "Update " + Server.SHORT_PRODUCT_NAME + " Password for Appliance Instance '" + this.dai.getName() + "'";
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.dai);
    }

}
