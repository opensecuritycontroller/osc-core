package org.osc.core.broker.service.tasks.conformance.virtualsystem;

import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.VirtualSystemPolicy;
import org.osc.core.broker.model.plugin.sdncontroller.VMwareSdnApiFactory;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osc.sdk.sdn.api.ServiceProfileApi;

public class DeleteDefaultServiceProfileTask extends TransactionalTask {
    private static final Logger log = Logger.getLogger(DeleteDefaultServiceProfileTask.class);

    private VirtualSystemPolicy vsp;

    public DeleteDefaultServiceProfileTask(VirtualSystemPolicy vsp) {
        this.vsp = vsp;
        this.name = getName();
    }

    @Override
    public void executeTransaction(Session session) throws Exception {
        log.debug("Start excecuting DeleteDefaultServiceProfileTask");

        this.vsp = (VirtualSystemPolicy) session.get(VirtualSystemPolicy.class, this.vsp.getId());
        ServiceProfileApi serviceProfileApi = VMwareSdnApiFactory.createServiceProfileApi(this.vsp.getVirtualSystem());
        serviceProfileApi.deleteServiceProfile(this.vsp.getVirtualSystem().getNsxServiceId(), this.vsp.getNsxVendorTemplateId());
        log.debug("Deleted service profile of the service: " + this.vsp.getVirtualSystem().getNsxServiceId() + " for vendor template: " + this.vsp.getNsxVendorTemplateId());
    }

    @Override
    public String getName() {
        return "Delete default Profile for Policy '" + this.vsp.getPolicy().getName() + "' in '"
                + this.vsp.getVirtualSystem().getVirtualizationConnector().getName() + "'";
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.vsp.getVirtualSystem());
    }

}
