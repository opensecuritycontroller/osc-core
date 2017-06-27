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
package org.osc.core.broker.rest.client.openstack.openstack4j;

import org.apache.log4j.Logger;
import org.openstack4j.api.OSClient;
import org.openstack4j.api.client.IOSClientBuilder;
import org.openstack4j.api.types.Facing;
import org.openstack4j.core.transport.Config;
import org.openstack4j.model.common.Identifier;
import org.openstack4j.openstack.OSFactory;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;

public class KeystoneProvider {

    private static final Logger log = Logger.getLogger(KeystoneProvider.class);

    private static final String KEYSTONE_VERSION = "v3";

    private static KeystoneProvider instance = null;
    private static OSClient.OSClientV3 os;
    private Endpoint endpoint;

    protected KeystoneProvider() {
    }

    private KeystoneProvider(Endpoint endpoint) {
        this.endpoint = endpoint;
    }

    public static KeystoneProvider getInstance(Endpoint endPoint) {
        if (instance == null || !instance.endpoint.equals(endPoint)) {
            instance = new KeystoneProvider(endPoint);
        }
        return instance;
    }

    OSClient.OSClientV3 getAvailableSession() {
        OSClient.OSClientV3 localOs;
        Config config = Config.newConfig().withSSLContext(this.endpoint.getSslContext()).withHostnameVerifier((hostname, session) -> true);
        if (os == null || instance.endpoint.getToken() == null) {
            String endpointURL;
            try {
                endpointURL = prepareEndpointURL(this.endpoint);
            } catch (URISyntaxException | MalformedURLException e) {
                throw new RuntimeException(e.getMessage(), e);
            }

            // LOGGER
            OSFactory.enableHttpLoggingFilter(log.isDebugEnabled() || log.isInfoEnabled());

            Identifier domainIdentifier = Identifier.byId(this.endpoint.getDomainId());

            IOSClientBuilder.V3 keystoneV3Builder = OSFactory.builderV3().perspective(Facing.ADMIN)
                    .endpoint(endpointURL)
                    .credentials(this.endpoint.getUser(), this.endpoint.getPassword(), domainIdentifier)
                    .scopeToProject(Identifier.byName(this.endpoint.getTenant()), domainIdentifier)
                    .withConfig(config);

            localOs = keystoneV3Builder.authenticate();
            instance.endpoint.setToken(localOs.getToken());
        } else {
            localOs = OSFactory.clientFromToken(instance.endpoint.getToken(), Facing.ADMIN, config);
        }

        os = localOs;
        return localOs;
    }

    private String prepareEndpointURL(Endpoint endPoint) throws URISyntaxException, MalformedURLException {
        String schema = endPoint.isHttps() ? "https" : "http";
        URI uri = new URI(schema, null, endPoint.getEndPointIP(), 5000, "/" + KEYSTONE_VERSION, null, null);
        return uri.toURL().toString();
    }

}
