package org.osc.core.broker.service.tasks.network;

import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.plugin.sdncontroller.VMwareSdnApiFactory;
import org.osc.core.broker.rest.server.AgentAuthFilter;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osc.core.util.EncryptionUtil;
import org.osc.core.util.ServerUtil;
import org.osc.sdk.sdn.api.ServiceInstanceApi;

public class UpdateNsxServiceInstanceAttributesTask extends TransactionalTask {
    private static final Logger log = Logger.getLogger(UpdateNsxServiceInstanceAttributesTask.class);

    private VirtualSystem vs;

    public UpdateNsxServiceInstanceAttributesTask(VirtualSystem vs) {
        this.vs = vs;
        this.name = getName();
    }

    @Override
    public void executeTransaction(Session session) throws Exception {

        log.info("Start executing UpdateNsxServiceInstanceAttributesTask");

        this.vs = (VirtualSystem) session.get(VirtualSystem.class, this.vs.getId());

        ServiceInstanceApi serviceInstanceApi = VMwareSdnApiFactory.createServiceInstanceApi(this.vs);

        serviceInstanceApi.updateServiceInstance(
                this.vs.getNsxServiceInstanceId(),
                this.vs.getNsxServiceId(),
                AgentAuthFilter.VMIDC_AGENT_LOGIN,
                EncryptionUtil.encryptAESCTR(AgentAuthFilter.VMIDC_AGENT_PASS),
                ServerUtil.getServerIP(),
                this.vs.getDistributedAppliance().getApplianceVersion(),
                this.vs.getDistributedAppliance().getAppliance().getModel());
    }

    @Override
    public String getName() {
        return "Updating Service Instance Attributes of NSX Manager'" + this.vs.getVirtualizationConnector().getName()
                + "'";
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.vs);
    }

}
