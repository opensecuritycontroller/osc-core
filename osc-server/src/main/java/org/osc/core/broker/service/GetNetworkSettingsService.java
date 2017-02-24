package org.osc.core.broker.service;

import org.hibernate.Session;
import org.osc.core.broker.service.dto.NetworkSettingsDto;
import org.osc.core.broker.service.request.GetNetworkSettingsRequest;
import org.osc.core.broker.service.response.GetNetworkSettingsResponse;
import org.osc.core.broker.util.network.NetworkSettingsApi;

public class GetNetworkSettingsService extends ServiceDispatcher<GetNetworkSettingsRequest, GetNetworkSettingsResponse> {

    @Override
    public GetNetworkSettingsResponse exec(GetNetworkSettingsRequest request, Session session) throws Exception {

        NetworkSettingsApi api = new NetworkSettingsApi();
        NetworkSettingsDto nsd = api.getNetworkSettings();

        GetNetworkSettingsResponse response = new GetNetworkSettingsResponse();
        response.setDhcp(nsd.isDhcp());
        response.setHostIpAddress(nsd.getHostIpAddress());
        response.setHostSubnetMask(nsd.getHostSubnetMask());
        response.setHostDefaultGateway(nsd.getHostDefaultGateway());
        response.setHostDnsServer1(nsd.getHostDnsServer1());
        response.setHostDnsServer2(nsd.getHostDnsServer2());

        return response;
    }

}
