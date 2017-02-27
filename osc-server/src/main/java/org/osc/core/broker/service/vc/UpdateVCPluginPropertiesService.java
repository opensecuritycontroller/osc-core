package org.osc.core.broker.service.vc;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.plugin.sdncontroller.ControllerType;
import org.osc.core.broker.service.ServiceDispatcher;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.persistence.VirtualizationConnectorEntityMgr;
import org.osc.core.broker.service.request.UpdateConnectorPluginPropertiesRequest;
import org.osc.core.broker.service.response.EmptySuccessResponse;

public class UpdateVCPluginPropertiesService extends
ServiceDispatcher<UpdateConnectorPluginPropertiesRequest, EmptySuccessResponse>{
    private static final Logger LOG = Logger.getLogger(UpdateVCPluginPropertiesService.class);

    @Override
    protected EmptySuccessResponse exec(UpdateConnectorPluginPropertiesRequest request, Session session)
            throws Exception {
        Map<String, Object> properties = request.getProperties();

        if (properties == null) {
            throw new VmidcBrokerValidationException("The provided properties map should not be null.");
        }

        if (StringUtils.isBlank(request.getPluginName())) {
            throw new VmidcBrokerValidationException("The provided plugin name should not be empty or null.");
        }

        EmptySuccessResponse response = new EmptySuccessResponse();

        List<VirtualizationConnector> vcs = VirtualizationConnectorEntityMgr.listByControllerType(session, ControllerType.fromText(request.getPluginName()));

        if (vcs == null || vcs.isEmpty()) {
            LOG.info(String.format("No virtualization connector found for the controller type %s. Noop.", request.getPluginName()));
            return response;
        }

        EntityManager<VirtualizationConnector> emgr = new EntityManager<>(VirtualizationConnector.class, session);

        for (VirtualizationConnector vc : vcs) {
            // TODO emanoel: set VC properties here.
            emgr.update(vc);
        }

        return response;
    }
}

