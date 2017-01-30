package org.osc.core.broker.service.mc;

import org.hibernate.Session;
import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.service.ConformService;
import org.osc.core.broker.service.ServiceDispatcher;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.request.SyncApplianceManagerConnectorRequest;
import org.osc.core.broker.service.response.SyncApplianceManagerConnectorResponse;

public class SyncManagerConnectorService extends
        ServiceDispatcher<SyncApplianceManagerConnectorRequest, SyncApplianceManagerConnectorResponse> {

    @Override
    public SyncApplianceManagerConnectorResponse exec(SyncApplianceManagerConnectorRequest request, Session session)
            throws Exception {

        EntityManager<ApplianceManagerConnector> emgr = new EntityManager<ApplianceManagerConnector>(
                ApplianceManagerConnector.class, session);

        ApplianceManagerConnector mc = emgr.findByPrimaryKey(request.getId());

        validate(request, mc);

        Long jobId = ConformService.startMCConformJob(mc, session).getId();

        SyncApplianceManagerConnectorResponse response = new SyncApplianceManagerConnectorResponse();
        response.setJobId(jobId);

        return response;
    }

    void validate(SyncApplianceManagerConnectorRequest request, ApplianceManagerConnector mc) throws Exception {

        // check for uniqueness of mc IP
        if (mc == null) {

            throw new VmidcBrokerValidationException("Appliance Manager Connector Id " + request.getId()
                    + " not found.");
        }

    }
}
