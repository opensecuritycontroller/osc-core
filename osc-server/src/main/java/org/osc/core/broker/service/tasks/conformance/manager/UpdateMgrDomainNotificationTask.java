package org.osc.core.broker.service.tasks.conformance.manager;

import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Session;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.model.plugin.manager.ManagerApiFactory;
import org.osc.core.broker.rest.server.VmidcAuthFilter;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osc.core.util.ServerUtil;
import org.osc.sdk.manager.api.ManagerCallbackNotificationApi;

import com.mcafee.vmidc.server.Server;

public class UpdateMgrDomainNotificationTask extends TransactionalTask {
    private static final Logger log = Logger.getLogger(UpdateMgrDomainNotificationTask.class);

    private ApplianceManagerConnector mc;
    private String oldBrokerIp;

    public UpdateMgrDomainNotificationTask(ApplianceManagerConnector mc, String oldBrokerIp) {
        this.mc = mc;
        this.name = getName();
        this.oldBrokerIp = oldBrokerIp;
    }

    @Override
    public void executeTransaction(Session session) throws Exception {
        log.debug("Start excecuting RegisterMgrDomainNotificationTask Task. MC: '" + this.mc.getName() + "'");

        this.mc = (ApplianceManagerConnector) session.get(ApplianceManagerConnector.class, this.mc.getId(),
                new LockOptions(LockMode.PESSIMISTIC_WRITE));
        ManagerCallbackNotificationApi mgrApi = null;
        try {
            mgrApi = ManagerApiFactory.createManagerUrlNotificationApi(this.mc);
            mgrApi.updateDomainNotificationRegistration(this.oldBrokerIp, Server.getApiPort(),
                    VmidcAuthFilter.VMIDC_DEFAULT_LOGIN, VmidcAuthFilter.VMIDC_DEFAULT_PASS);

            this.mc.setLastKnownNotificationIpAddress(ServerUtil.getServerIP());
            EntityManager.update(session, this.mc);

        } finally {

            if (mgrApi != null) {
                mgrApi.close();
            }
        }
    }

    @Override
    public String getName() {
        return "Update Domain Notifications for Manager Connector '" + this.mc.getName() + "'";
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.mc);
    }

}
