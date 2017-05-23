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

public class OscRestServlet {

    // Because we started with versions in the URL, we will continue to have v1 in the URL
    public static final String SERVER_API_PATH_PREFIX = "/api/server/v1";
    public static final String AGENT_API_PATH_PREFIX = "/api/agent/v1";
    public static final String MANAGER_API_PATH_PREFIX = "/api/manager/v1";

    // Legacy/Proprietary API
    public static final String MGR_NSM_API_PATH_PREFIX = "/api/nsm/v1";
}
