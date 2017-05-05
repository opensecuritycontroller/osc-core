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
package org.osc.core.broker.rest.server;

import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.StringTokenizer;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.ext.Provider;

import org.osc.core.broker.rest.RestConstants;
import org.osc.core.broker.util.PasswordUtil;
import org.osc.core.rest.annotations.OscAuth;
import org.osc.core.util.AuthUtil;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = OscAuthFilter.class)
@Provider
@OscAuth
public class OscAuthFilter implements ContainerRequestFilter {
    @Reference
    private PasswordUtil passwordUtil;

    @Override
    public void filter(ContainerRequestContext containerRequestContext) throws IOException {
        AuthUtil.authenticate(containerRequestContext, RestConstants.OSC_DEFAULT_LOGIN, this.passwordUtil.getOscDefaultPass());
    }

    public static String getUsername(HttpHeaders headers) {
        List<String> authorizationHeader = headers.getRequestHeader("Authorization");

        if (authorizationHeader == null || authorizationHeader.size() == 0) {
            throw new IllegalArgumentException("Basic authorization is not set");
        }

        String authString = authorizationHeader.get(0);
        // Get encoded username and password
        final String encodedUserPassword = authString.replaceFirst("Basic ", "");

        // Decode username and password
        String usernameAndPassword = new String(Base64.getDecoder().decode(encodedUserPassword));

        // Split username and password tokens
        final StringTokenizer tokenizer = new StringTokenizer(usernameAndPassword, ":");
        return tokenizer.nextToken();
    }
}
