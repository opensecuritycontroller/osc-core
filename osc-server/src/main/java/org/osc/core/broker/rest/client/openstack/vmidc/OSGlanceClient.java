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

import org.apache.log4j.Logger;
import org.osc.core.broker.rest.client.RestBaseClient;
import org.osc.core.broker.rest.client.openstack.jcloud.Endpoint;

public class OSGlanceClient extends RestBaseClient {
    private static final String OPENSTACK_REST_URL_BASE = "/v2";
    public static final int GLANCE_PORT = 9292;

    Logger log = Logger.getLogger(OSGlanceClient.class);

    public OSGlanceClient(Endpoint endPoint, String token) {

        super(OPENSTACK_REST_URL_BASE, MediaType.APPLICATION_JSON);

        initRestBaseClient(endPoint.getEndPointIP(), GLANCE_PORT, endPoint.getUser(), endPoint.getPassword(),
                endPoint.isHttps());

        this.headerKey = "X-Auth-Token";
        this.headerKeyValue = token;
    }

}
