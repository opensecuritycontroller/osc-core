package org.osc.core.broker.service;

import org.hibernate.Session;
import org.osc.core.broker.service.persistence.DistributedApplianceInstanceEntityMgr;
import org.osc.core.broker.service.request.Request;
import org.osc.core.broker.service.response.CheckNetworkSettingResponse;

public class CheckNetworkSettingsService extends ServiceDispatcher<Request, CheckNetworkSettingResponse> {

    @Override
    public CheckNetworkSettingResponse exec(Request request, Session session) throws Exception {
        CheckNetworkSettingResponse response = new CheckNetworkSettingResponse();
        response.setHasDeployedInstances(DistributedApplianceInstanceEntityMgr.doesDAIExist(session));
        return response;
    }

}
