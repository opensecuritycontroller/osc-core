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
package org.osc.core.broker.rest.client.openstack.vmidc.api;

import org.openstack4j.model.identity.v3.Token;
import org.osc.core.broker.rest.client.openstack.openstack4j.Endpoint;
import org.osc.core.broker.rest.client.openstack.openstack4j.Openstack4jAuthentication;
import org.osc.core.broker.rest.client.openstack.vmidc.OSGlanceClient;

import java.io.IOException;

public class OSGlanceRestApi {

    private Token token;
    protected OSGlanceClient osGlanceClient;

    public OSGlanceRestApi(Endpoint endPoint) throws IOException {
        try (Openstack4jAuthentication authApi = new Openstack4jAuthentication(endPoint)) {
            this.token = authApi.getProjectToken();
        }
        this.osGlanceClient = new OSGlanceClient(endPoint, this.token.getId());
    }

}
