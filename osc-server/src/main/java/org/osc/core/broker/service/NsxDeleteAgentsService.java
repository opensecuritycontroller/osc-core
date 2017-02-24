package org.osc.core.broker.service;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.DistributedApplianceInstanceEntityMgr;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.request.NsxDeleteAgentsRequest;
import org.osc.core.broker.service.response.EmptySuccessResponse;
import org.osc.core.broker.service.tasks.conformance.manager.MgrDeleteMemberDeviceTask;

public class NsxDeleteAgentsService extends ServiceDispatcher<NsxDeleteAgentsRequest, EmptySuccessResponse> {

    private static final Logger log = Logger.getLogger(NsxDeleteAgentsService.class);

    @Override
    public EmptySuccessResponse exec(NsxDeleteAgentsRequest request, Session session) throws Exception {

        EntityManager<DistributedApplianceInstance> emgr = new EntityManager<DistributedApplianceInstance>(
                DistributedApplianceInstance.class, session);

        DistributedApplianceInstance dai = validate(session, request, emgr);

        if (dai != null) {
            if (MgrDeleteMemberDeviceTask.deleteMemberDevice(dai)) {
                EntityManager.delete(session, dai);
            }
        } else {
            log.info("An unregistered nsx appliance agent '" + request.agentIds + "' had been undeployed.");
        }

        EmptySuccessResponse response = new EmptySuccessResponse();

        return response;
    }

    private DistributedApplianceInstance validate(Session session, NsxDeleteAgentsRequest request,
            EntityManager<DistributedApplianceInstance> emgr) throws Exception {

        DistributedApplianceInstance dai = null;

        if (request.nsxIpAddress == null) {
            throw new VmidcBrokerValidationException("Missing NSX IP Address.");
        }

        if (request.agentIds == null) {
            throw new VmidcBrokerValidationException("Missing Agent IDs.");
        }

        dai = DistributedApplianceInstanceEntityMgr.findByNsxAgentIdAndNsxIp(session, request.agentIds,
                request.nsxIpAddress);
        return dai;
    }

}
