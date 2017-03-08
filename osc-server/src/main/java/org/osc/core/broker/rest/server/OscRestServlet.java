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

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.osc.core.broker.rest.server.api.AlarmApis;
import org.osc.core.broker.rest.server.api.AlertApis;
import org.osc.core.broker.rest.server.api.ApplianceApis;
import org.osc.core.broker.rest.server.api.DistributedApplianceApis;
import org.osc.core.broker.rest.server.api.DistributedApplianceInstanceApis;
import org.osc.core.broker.rest.server.api.JobApis;
import org.osc.core.broker.rest.server.api.ManagerApis;
import org.osc.core.broker.rest.server.api.ManagerConnectorApis;
import org.osc.core.broker.rest.server.api.ServerDebugApis;
import org.osc.core.broker.rest.server.api.ServerMgmtApis;
import org.osc.core.broker.rest.server.api.VirtualSystemApis;
import org.osc.core.broker.rest.server.api.VirtualizationConnectorApis;
import org.osc.core.broker.rest.server.api.proprietary.NsmMgrApis;
import org.osc.core.broker.rest.server.api.proprietary.NsxApis;
import org.osc.core.util.LocalHostAuthFilter;

import javax.ws.rs.ApplicationPath;

/**
 * The Main rest servlet class which dispatches requests to the API classes based on their path parameters
 */
@ApplicationPath("")
public class OscRestServlet extends ResourceConfig {

    // Because we started with versions in the URL, we will continue to have v1 in the URL
    public static final String SERVER_API_PATH_PREFIX = "/api/server/v1";
    public static final String AGENT_API_PATH_PREFIX = "/api/agent/v1";
    public static final String MANAGER_API_PATH_PREFIX = "/api/manager/v1";

    // Legacy/Proprietary API
    public static final String NSX_API_PATH_PREFIX = "/api/nsx/vmware/2.0";
    public static final String MGR_NSM_API_PATH_PREFIX = "/api/nsm/v1";

    /**
     * Defining where to look for server API
     * */
    public OscRestServlet() {
        //Json feature
        register(com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider.class);

        //Agent Api
        register(NsxApis.class);
        register(NsmMgrApis.class);
        register(ManagerApis.class);

        // Server APIs
        register(ServerMgmtApis.class);
        register(JobApis.class);
        register(VirtualizationConnectorApis.class);
        register(ManagerConnectorApis.class);
        register(ApplianceApis.class);
        register(DistributedApplianceApis.class);
        register(DistributedApplianceInstanceApis.class);
        register(VirtualSystemApis.class);
        register(AlarmApis.class);
        register(AlertApis.class);
        register(ServerDebugApis.class);

        //Auth Filters
        register(AgentAuthFilter.class);
        register(NsxAuthFilter.class);
        register(LocalHostAuthFilter.class);
        register(OscAuthFilter.class);

        //Properties
        property(ServerProperties.FEATURE_AUTO_DISCOVERY_DISABLE, true);
    }
}