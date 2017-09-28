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
package org.osc.core.broker.util;

import java.util.Base64;
import java.util.Map;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Response;

import org.osc.core.broker.service.api.server.EncryptionApi;
import org.osc.core.broker.service.api.server.EncryptionException;
import org.osc.core.broker.service.api.server.LoggingApi;
import org.osc.core.broker.util.log.LogProvider;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;

import com.google.common.collect.ImmutableMap;

@Component(service=AuthUtil.class)
public class AuthUtil {

    @Reference
    EncryptionApi encrypter;

    @Reference
    LoggingApi logging;

    private static final Logger log = LogProvider.getLogger(AuthUtil.class);

    public void authenticate(ContainerRequestContext request, String validUserName, String validPass) {
        authenticate(request, ImmutableMap.of(validUserName, validPass));
    }

    /**
     * Checks if the request contains the username password combination from the given map of username and password.
     *
     */
    public void authenticate(ContainerRequestContext request, Map<String, String> usernamePasswordMap) {

        String authHeader = request.getHeaderString("Authorization");
        WebApplicationException wae = new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED)
                .header("WWW-Authenticate", "Basic").entity("not authorized").build());

        String requestUri = request.getUriInfo()!=null?request.getUriInfo().getBaseUri().toString():"";
        if (authHeader == null) {
            log.warn("Authentication of " + requestUri + " failed as auth header was null");
            throw wae;
        } else {
            String[] tokens = authHeader.trim().split("\\s+");

            if (tokens.length != 2 || !tokens[0].equalsIgnoreCase("BASIC")) {
                log.warn("Authentication of " + requestUri + " failed as auth header does not have the right tokens");
                throw wae;
            }

            // valid auth header format, now need to authenticate the right user
            byte[] decodedBytes = Base64.getDecoder().decode(tokens[1]);
            String credString = new String(decodedBytes);

            String[] credentials = credString.split(":");

            if (credentials.length != 2) {
                log.warn("Authentication of " + this.logging.removeCRLF(requestUri) + " failed - invalid credentials format");
                throw wae;
            }

            String loginName = credentials[0];
            String password = credentials[1];

            if (!validateUserAndPassword(loginName, password, usernamePasswordMap)) {
                log.warn("Authentication of " + this.logging.removeCRLF(requestUri) + " failed - user password mismatch");
                throw wae;
            }
        }
    }

    private boolean validateUserAndPassword(String loginName, String password, Map<String, String> usernamePasswordMap) {
        return usernamePasswordMap.entrySet().stream().anyMatch(entry -> {
            try {
                return entry.getKey().equalsIgnoreCase(loginName) && this.encrypter.validateAESCTR(password, entry.getValue());
            } catch (EncryptionException encryptionException) {
                log.error("Failed to validate AESCTR password", encryptionException);
                return false;
            }
        });
    }

    public void authenticateLocalRequest(ContainerRequestContext request) {
        WebApplicationException wae = new WebApplicationException(
                Response.status(Response.Status.UNAUTHORIZED).entity("not authorized").build());
        if(request.getUriInfo()!=null && request.getUriInfo().getRequestUri()!=null) {
            String remoteAddr = request.getUriInfo().getRequestUri().getHost();
            if (remoteAddr == null || !(remoteAddr.equals("127.0.0.1") || remoteAddr.equals("localhost"))) {
                throw wae;
            }
        } else {
            throw wae;
        }

    }
}
