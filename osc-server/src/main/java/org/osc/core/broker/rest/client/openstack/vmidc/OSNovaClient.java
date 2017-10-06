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
package org.osc.core.broker.rest.client.openstack.vmidc;

import javax.ws.rs.core.MediaType;

import org.osc.core.broker.rest.client.RestBaseClient;
import org.osc.core.broker.rest.client.openstack.openstack4j.Endpoint;
import org.osc.core.broker.util.log.LogProvider;
import org.slf4j.Logger;

public class OSNovaClient extends RestBaseClient {
    private static final String OPENSTACK_REST_URL_BASE = "/v2";
    public static final int NOVA_PORT = 8774;

    Logger log = LogProvider.getLogger(OSNovaClient.class);

    public OSNovaClient(Endpoint endPoint, String token) {

        super(OPENSTACK_REST_URL_BASE, MediaType.APPLICATION_JSON);

        initRestBaseClient(endPoint.getEndPointIP(), NOVA_PORT, endPoint.getUser(), endPoint.getPassword(),
                endPoint.isHttps());

        this.headerKey = "X-Auth-Token";
        this.headerKeyValue = token;
    }

}
