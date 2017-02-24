package org.osc.core.broker.service.mc;

import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.osc.core.broker.model.entities.appliance.DistributedAppliance;
import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.service.ConformService;
import org.osc.core.broker.service.ServiceDispatcher;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.DistributedApplianceEntityMgr;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.request.MCChangeNotificationRequest;
import org.osc.core.broker.service.response.BaseJobResponse;
import org.osc.sdk.manager.element.MgrChangeNotification.MgrObjectType;

public class MCChangeNotificationService extends
ServiceDispatcher<MCChangeNotificationRequest, BaseJobResponse> {

    private static final Logger log = Logger.getLogger(MCChangeNotificationService.class);

    @Override
    public BaseJobResponse exec(MCChangeNotificationRequest request, Session session) throws Exception {

        BaseJobResponse response = new BaseJobResponse();

        EntityManager<ApplianceManagerConnector> emgr = new EntityManager<ApplianceManagerConnector>(
                ApplianceManagerConnector.class, session);

        ApplianceManagerConnector mc = validate(session, request, emgr);

        response.setId(mc.getId());
        List<DistributedAppliance> distributedAppliances = DistributedApplianceEntityMgr.listActiveByManagerConnector(
                session, mc);

        if (distributedAppliances.isEmpty()) {
            response.setJobId(ConformService.startMCConformJob(mc, session).getId());
        } else if (request.notification.getObjectType() == MgrObjectType.DOMAIN) {
            // TODO: Future. Need to make this more efficient. Create a job that only
            // update domain/policy
        } else if (request.notification.getObjectType() == MgrObjectType.POLICY) {

            for (DistributedAppliance da : distributedAppliances) {
                Long jobId = ConformService.startDAConformJob(session, da);
                response.setJobId(jobId);
                log.info("Sync DA '" + da.getName() + "' job " + jobId + " triggered.");
            }
        }

        return response;
    }

    private ApplianceManagerConnector validate(Session session, MCChangeNotificationRequest request,
            EntityManager<ApplianceManagerConnector> emgr) throws Exception {

        if (request.mgrIpAddress == null) {
            throw new VmidcBrokerValidationException("Missing Manager IP Address.");
        }

        if (request.notification == null) {
            throw new VmidcBrokerValidationException("Invalid notification.");
        }

        // retrieve existing entry from db
        ApplianceManagerConnector mc = emgr.findByFieldName("ipAddress", request.mgrIpAddress);
        if (mc == null) {
            throw new VmidcBrokerValidationException("Manager with IP address '" + request.mgrIpAddress
                    + "' not found.");
        }

        return mc;
    }

}
