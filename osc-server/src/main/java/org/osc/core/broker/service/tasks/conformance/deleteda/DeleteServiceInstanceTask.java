package org.osc.core.broker.service.tasks.conformance.deleteda;

import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.plugin.sdncontroller.VMwareSdnApiFactory;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osc.sdk.sdn.api.ServiceInstanceApi;

public class DeleteServiceInstanceTask extends TransactionalTask {
    private static final Logger LOG = Logger.getLogger(DeleteServiceInstanceTask.class);

    private VirtualSystem vs;

    public DeleteServiceInstanceTask(VirtualSystem vs) {
        this.vs = vs;
        this.name = getName();
    }

    @Override
    public void executeTransaction(Session session) throws Exception {

        LOG.debug("Start executing DeleteServiceInstance task");

        String siId = this.vs.getNsxServiceInstanceId();
        // delete service instance
        ServiceInstanceApi serviceInstanceApi = VMwareSdnApiFactory.createServiceInstanceApi(this.vs);
        serviceInstanceApi.deleteServiceInstance(this.vs.getNsxServiceInstanceId(), this.vs.getNsxServiceId());

        LOG.debug("Updating nsx si " + siId + " for VirtualSystem: " + this.vs.getId());
        this.vs = (VirtualSystem) session.get(VirtualSystem.class, this.vs.getId());
        this.vs.setNsxServiceInstanceId(null);
        EntityManager.update(session, this.vs);
    }

    @Override
    public String getName() {
        return "Delete Service Instance '" + this.vs.getVirtualizationConnector().getName() + "'";
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.vs);
    }

}
