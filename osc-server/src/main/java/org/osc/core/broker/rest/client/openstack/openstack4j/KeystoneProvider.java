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

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.openstack4j.api.OSClient;
import org.openstack4j.api.client.IOSClientBuilder;
import org.openstack4j.api.types.Facing;
import org.openstack4j.core.transport.Config;
import org.openstack4j.model.common.Identifier;
import org.openstack4j.model.identity.v3.Token;
import org.openstack4j.openstack.OSFactory;

public final class KeystoneProvider {

    private static final Logger log = Logger.getLogger(KeystoneProvider.class);

    private static final String KEYSTONE_VERSION = "v3";

    private static KeystoneProvider instance = null;
    private static HashMap<Endpoint, Token> connectionsMap = new HashMap<>();

    private KeystoneProvider() {
    }

    public static KeystoneProvider getInstance() {
        if (instance == null) {
            instance = new KeystoneProvider();
        }
        return instance;
    }

    OSClient.OSClientV3 getAvailableSession(Endpoint endpoint) {

        OSClient.OSClientV3 localOs;
        Config config = Config.newConfig().withSSLContext(endpoint.getSslContext()).withHostnameVerifier((hostname, session) -> true);
        if (connectionsMap.containsKey(endpoint)) {
            localOs = OSFactory.clientFromToken(connectionsMap.get(endpoint), Facing.ADMIN, config);
        } else {
            String endpointURL;
            try {
                endpointURL = prepareEndpointURL(endpoint);
            } catch (URISyntaxException | MalformedURLException e) {
                throw new RuntimeException(e.getMessage(), e);
            }

            // LOGGER
            OSFactory.enableHttpLoggingFilter(log.isDebugEnabled() || log.isInfoEnabled());

            Identifier domainIdentifier = Identifier.byId(endpoint.getDomainId());

            IOSClientBuilder.V3 keystoneV3Builder = OSFactory.builderV3().perspective(Facing.ADMIN)
                    .endpoint(endpointURL)
                    .credentials(endpoint.getUser(), endpoint.getPassword(), domainIdentifier)
                    .scopeToProject(Identifier.byName(endpoint.getProject()), domainIdentifier)
                    .withConfig(config);

            localOs = keystoneV3Builder.authenticate();
            connectionsMap.put(endpoint, localOs.getToken());
        }

        return localOs;
    }

    private String prepareEndpointURL(Endpoint endPoint) throws URISyntaxException, MalformedURLException {
        String schema = endPoint.isHttps() ? "https" : "http";
        URI uri = new URI(schema, null, endPoint.getEndPointIP(), 5000, "/" + KEYSTONE_VERSION, null, null);
        return uri.toURL().toString();
    }

    void clearConnectionMap(Endpoint endpoint) {
        connectionsMap.remove(endpoint);
    }

}
