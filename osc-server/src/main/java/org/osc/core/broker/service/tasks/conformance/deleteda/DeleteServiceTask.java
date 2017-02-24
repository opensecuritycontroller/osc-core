package org.osc.core.broker.service.tasks.conformance.deleteda;

import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.plugin.sdncontroller.VMwareSdnApiFactory;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osc.sdk.sdn.api.ServiceApi;
import org.osc.sdk.sdn.element.ServiceElement;

public class DeleteServiceTask extends TransactionalTask {
    private static final Logger LOG = Logger.getLogger(DeleteServiceTask.class);

    private VirtualSystem vs;

    public DeleteServiceTask(VirtualSystem vs) {
        this.vs = vs;
        this.name = getName();
    }

    @Override
    public void executeTransaction(Session session) throws Exception {

        LOG.debug("Start Executing DeleteServiceTask Task for vs " + this.vs.getId());

        // delete service

        String sId = this.vs.getNsxServiceId();

        ServiceApi serviceApi = VMwareSdnApiFactory.createServiceApi(this.vs);

        try {
            serviceApi.deleteService(sId);
        } catch (Exception e) {
            ServiceElement service = serviceApi.findService(this.vs.getDistributedAppliance().getName());
            if (service != null) {
                throw e;
            }
        }

        LOG.debug("Updating nsx svc " + sId + " for VirtualSystem: " + this.vs.getId());
        this.vs = (VirtualSystem) session.get(VirtualSystem.class, this.vs.getId());
        this.vs.setNsxServiceId(null);
        EntityManager.update(session, this.vs);
    }

    @Override
    public String getName() {
        return "Delete Service '" + this.vs.getVirtualizationConnector().getName() + "'";
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.vs);
    }

}
