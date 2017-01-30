package org.osc.core.broker.service.tasks.conformance.virtualsystem;

import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.plugin.manager.ManagerApiFactory;
import org.osc.core.broker.model.plugin.sdncontroller.VMwareSdnApiFactory;
import org.osc.core.broker.rest.client.nsx.model.ServiceManager;
import org.osc.core.broker.rest.server.NsxAuthFilter;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osc.core.util.ServerUtil;
import org.osc.sdk.sdn.api.ServiceManagerApi;
import org.osc.sdk.sdn.element.ServiceManagerElement;

import com.mcafee.vmidc.server.Server;

public class CreateNsxServiceManagerTask extends TransactionalTask {
    private static final Logger LOG = Logger.getLogger(CreateNsxServiceManagerTask.class);

    private VirtualSystem vs;

    public CreateNsxServiceManagerTask(VirtualSystem vs) {
        this.vs = vs;
        this.name = getName();
    }

    @Override
    public void executeTransaction(Session session) throws Exception {
        LOG.debug("Start Executing RegisterServiceManager Task for vs: " + this.vs.getId());

        this.vs = (VirtualSystem) session.get(VirtualSystem.class, this.vs.getId());

        ServiceManagerElement serviceManager = null;
        ServiceManagerApi serviceManagerApi = VMwareSdnApiFactory.createServiceManagerApi(this.vs);

        String serviceManagerName = generateServiceManagerName(this.vs);

        ServiceManager input = new ServiceManager(
                serviceManagerName,
                serviceManagerName,
                serviceManagerName,
                buildRestCallbackUrl(),
                NsxAuthFilter.VMIDC_NSX_LOGIN,
                NsxAuthFilter.VMIDC_NSX_PASS,
                NsxAuthFilter.VMIDC_NSX_PASS);

        String serviceManagerId = serviceManagerApi.createServiceManager(input);
        serviceManager = serviceManagerApi.getServiceManager(serviceManagerId);

        this.vs.setNsxServiceManagerId(serviceManager.getId());
        this.vs.setNsxVsmUuid(serviceManager.getVsmId());
        EntityManager.update(session, this.vs);
    }

    public static String buildRestCallbackUrl() {
        return "https://" + ServerUtil.getServerIP() + ":" + Server.getApiPort() + "/api/nsx";
    }

    @Override
    public String getName() {
        return "Register Service Manager '" + this.vs.getVirtualizationConnector().getName() + "'";
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.vs);
    }

    public static String generateServiceManagerName(VirtualSystem vs) throws Exception {
        return "OSC " +
                ManagerApiFactory.createApplianceManagerApi(vs).getVendorName() +
                " " + vs.getDistributedAppliance().getName();
    }
}
