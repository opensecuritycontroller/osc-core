package org.osc.core.broker.service.tasks.network;

import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.rest.server.AgentAuthFilter;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osc.core.rest.client.agent.api.VmidcAgentApi;
import org.osc.core.util.NetworkUtil;

import com.mcafee.vmidc.server.Server;



public class IpChangePropagateToDaiTask extends TransactionalTask {

    final Logger log = Logger.getLogger(IpChangePropagateToDaiTask.class);

    private DistributedApplianceInstance dai;

    public IpChangePropagateToDaiTask(DistributedApplianceInstance dai) {
        this.dai = dai;
        this.name = getName();
    }

    @Override
    public void executeTransaction(Session session) throws Exception {
        this.dai = (DistributedApplianceInstance) session.get(DistributedApplianceInstance.class, this.dai.getId());

        VmidcAgentApi agentApi = new VmidcAgentApi(this.dai.getIpAddress(), 8090, AgentAuthFilter.VMIDC_AGENT_LOGIN,
                AgentAuthFilter.VMIDC_AGENT_PASS);

        agentApi.updateVmidcServerIp(NetworkUtil.getHostIpAddress());
    }

    @Override
    public String getName() {
        return "Update " + Server.SHORT_PRODUCT_NAME + " IP for Appliance Instance '" + this.dai.getName() + "'";
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.dai);
    }

}
