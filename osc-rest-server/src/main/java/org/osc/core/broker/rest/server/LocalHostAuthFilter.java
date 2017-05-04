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

import org.glassfish.jersey.server.ContainerRequest;
import org.osc.core.broker.rest.server.annotations.LocalHostAuth;
import org.osc.core.util.AuthUtil;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;

/**
 * Auth filter which makes sure the request is coming from the same machine where the request is being processed
 * (localhost or 127.0.0.1)
 */
@Provider
@LocalHostAuth
public class LocalHostAuthFilter implements ContainerRequestFilter {
    @Override
    public void filter(ContainerRequestContext containerRequestContext) throws IOException {
        AuthUtil.authenticateLocalRequest((ContainerRequest) containerRequestContext);
    }
}
