package org.osc.core.broker.service.tasks.mgrfile;

import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.rest.server.AgentAuthFilter;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osc.core.rest.client.agent.api.VmidcAgentStreamingApi;



public class MgrFileChangePropagateToDaiTask extends TransactionalTask {

    final Logger log = Logger.getLogger(MgrFileChangePropagateToDaiTask.class);

    private DistributedApplianceInstance dai;
    private byte[] mgrFile = null;
    private String mgrFileName = null;

    public DistributedApplianceInstance getDai() {
        return this.dai;
    }

    public MgrFileChangePropagateToDaiTask(DistributedApplianceInstance dai, byte[] mgrFile, String mgrFileName) {
        this.dai = dai;
        this.name = getName();
        this.mgrFile = mgrFile;
        this.mgrFileName = mgrFileName;
    }

    @Override
    public void executeTransaction(Session session) throws Exception {

        VmidcAgentStreamingApi agentApi = new VmidcAgentStreamingApi(this.dai.getIpAddress(), 8090,
                AgentAuthFilter.VMIDC_AGENT_LOGIN, AgentAuthFilter.VMIDC_AGENT_PASS);

        agentApi.updateMgrFile(this.mgrFile, this.mgrFileName);
    }

    @Override
    public String getName() {
        return "Propagate Manager File to DAI '" + this.dai.getName() + "'";
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(dai);
    }

}
