package org.osc.core.broker.service.tasks.conformance.deleteda;

import java.util.Set;

import org.hibernate.Session;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.plugin.sdncontroller.VMwareSdnApiFactory;
import org.osc.core.broker.rest.client.nsx.model.ServiceManager;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osc.sdk.sdn.api.ServiceManagerApi;
import org.osc.sdk.sdn.element.ServiceManagerElement;

public class UnregisterServiceManagerCallbackTask extends TransactionalTask {
    private VirtualSystem vs;

    public UnregisterServiceManagerCallbackTask(VirtualSystem vs) {
        this.vs = vs;
        this.name = getName();
    }

    @Override
    public void executeTransaction(Session session) throws Exception {
        ServiceManagerApi serviceManagerApi = VMwareSdnApiFactory.createServiceManagerApi(this.vs);
        ServiceManagerElement serviceManagerElement = serviceManagerApi.getServiceManager(this.vs.getNsxServiceManagerId());
        ServiceManager serviceManager = new ServiceManager(serviceManagerElement);
        serviceManager.setRestUrl(null);
        serviceManager.setLogin(null);
        serviceManager.setPassword(null);
        serviceManager.setVerifyPassword(null);

        serviceManagerApi.updateServiceManager(serviceManager);
    }

    @Override
    public String getName() {
        return "Remove callbacks registration from Service Manager '" + this.vs.getVirtualizationConnector().getName() + "'";
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.vs);
    }

}
