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

import com.google.common.collect.ImmutableMap;
import org.osc.core.rest.annotations.AgentAuth;
import org.osc.core.util.AuthUtil;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.ext.Provider;

import java.io.IOException;

@Provider
@AgentAuth
public class AgentAuthFilter implements ContainerRequestFilter {

    public static final String VMIDC_AGENT_LOGIN = "agent";
    public static String VMIDC_AGENT_PASS = "";

    @Override
    public void filter(ContainerRequestContext containerRequestContext) throws IOException {
        AuthUtil.authenticate(containerRequestContext, ImmutableMap.of(VMIDC_AGENT_LOGIN, VMIDC_AGENT_PASS,
                OscAuthFilter.OSC_DEFAULT_LOGIN, OscAuthFilter.OSC_DEFAULT_PASS));
    }

}
