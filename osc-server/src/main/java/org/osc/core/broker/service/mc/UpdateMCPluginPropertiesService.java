package org.osc.core.broker.service.mc;

import static org.osc.sdk.manager.Constants.SERVICE_NAME;
import static org.osc.sdk.manager.Constants.VENDOR_NAME;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.broker.model.plugin.manager.ManagerType;
import org.osc.core.broker.service.ServiceDispatcher;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.persistence.ApplianceManagerConnectorEntityMgr;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.service.request.UpdateConnectorPluginPropertiesRequest;
import org.osc.core.broker.service.response.EmptySuccessResponse;

public class UpdateMCPluginPropertiesService extends
ServiceDispatcher<UpdateConnectorPluginPropertiesRequest, EmptySuccessResponse>{
    private static final Logger LOG = Logger.getLogger(UpdateMCPluginPropertiesService.class);

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

        List<ApplianceManagerConnector> mcs = ApplianceManagerConnectorEntityMgr.listByManagerType(session, ManagerType.fromText(request.getPluginName()));

        if (mcs == null || mcs.isEmpty()) {
            LOG.info(String.format("No manager connector found for the type %s. Noop.", request.getPluginName()));
            return response;
        }

        EntityManager<ApplianceManagerConnector> emgr = new EntityManager<>(ApplianceManagerConnector.class, session);

        for (ApplianceManagerConnector mc : mcs) {
            mc.setVendorName((String)properties.get(VENDOR_NAME));
            mc.setServiceType((String)properties.get(SERVICE_NAME));
            // TODO emanoel: add other properties
            emgr.update(mc);
        }

        return response;
    }
}
