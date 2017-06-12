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

import java.io.IOException;

import org.apache.log4j.Logger;
import org.jclouds.openstack.keystone.v2_0.domain.Token;
import org.osc.core.broker.rest.client.openstack.jcloud.Endpoint;
import org.osc.core.broker.rest.client.openstack.jcloud.JCloudAuthentication;
import org.osc.core.broker.rest.client.openstack.vmidc.OSGlanceClient;

public class OSGlanceRestApi {
    Logger log = Logger.getLogger(OSGlanceRestApi.class);

    private Token token;
    protected OSGlanceClient osGlanceClient;

    public OSGlanceRestApi(Endpoint endPoint) throws IOException {

        JCloudAuthentication authApi = new JCloudAuthentication(endPoint);

        this.token = authApi.getTenantAccess().getToken();
        this.osGlanceClient = new OSGlanceClient(endPoint, this.token.getId());
        authApi.close();
    }

}
