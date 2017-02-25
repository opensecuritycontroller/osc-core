/*******************************************************************************
 * Copyright (c) 2017 Intel Corporation
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
package org.osc.core.rest.client;

import javax.ws.rs.core.MediaType;

public class VmidcServerAgentRestClient extends RestBaseClient {

    private static final String VMIDC_SERVER_REST_URL_BASE = "/api/agent/v1";

    public VmidcServerAgentRestClient(String vmiDCServer, String loginName, String password) {

        super(VMIDC_SERVER_REST_URL_BASE, MediaType.APPLICATION_XML);

        // If IP address does include port, we'll default.
        if (!vmiDCServer.contains(":")) {
            vmiDCServer += ":8090";
        }

        initRestBaseClient(vmiDCServer, 0, loginName, password, true);
    }
}
