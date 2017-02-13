package org.osc.core.broker.service.tasks.conformance.manager;

import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Session;
import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.model.plugin.manager.ManagerApiFactory;
import org.osc.core.broker.rest.server.OscAuthFilter;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osc.core.util.ServerUtil;
import org.osc.sdk.manager.api.ManagerCallbackNotificationApi;

import com.mcafee.vmidc.server.Server;

public class RegisterMgrDomainNotificationTask extends TransactionalTask {
    private static final Logger log = Logger.getLogger(RegisterMgrDomainNotificationTask.class);

    private ApplianceManagerConnector mc;

    public RegisterMgrDomainNotificationTask(ApplianceManagerConnector mc) {
        this.mc = mc;
        this.name = getName();
    }

    @Override
    public void executeTransaction(Session session) throws Exception {
        log.debug("Start excecuting RegisterMgrDomainNotificationTask Task. MC: '" + this.mc.getName() + "'");

        this.mc = (ApplianceManagerConnector) session.get(ApplianceManagerConnector.class, this.mc.getId(),
                new LockOptions(LockMode.PESSIMISTIC_WRITE));
        ManagerCallbackNotificationApi mgrApi = null;
        try {
            mgrApi = ManagerApiFactory.createManagerUrlNotificationApi(this.mc);
            mgrApi.createDomainNotificationRegistration(Server.getApiPort(), OscAuthFilter.OSC_DEFAULT_LOGIN,
                    OscAuthFilter.OSC_DEFAULT_PASS);
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
        return "Register Domain Notifications for Manager Connector '" + this.mc.getName() + "'";
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.mc);
    }

}
