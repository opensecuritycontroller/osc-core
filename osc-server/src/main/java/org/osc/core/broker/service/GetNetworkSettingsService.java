/*******************************************************************************
 * Copyright (c) Intel Corporation
 * Copyright (c) 2017
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
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
