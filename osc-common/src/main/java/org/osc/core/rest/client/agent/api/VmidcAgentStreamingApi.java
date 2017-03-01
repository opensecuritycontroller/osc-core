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
package org.osc.core.rest.client.agent.api;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import javax.ws.rs.core.MediaType;

import org.apache.commons.io.IOUtils;
import org.osc.core.rest.client.RestBaseClient;

public class VmidcAgentStreamingApi extends RestBaseClient {

    private static final String VMIDC_SERVER_REST_URL_BASE = "/api/agent/v1";

    public VmidcAgentStreamingApi(String agentServer, int port, String loginName, String password) {

        super(VMIDC_SERVER_REST_URL_BASE, MediaType.APPLICATION_OCTET_STREAM);

        initRestBaseClient(agentServer, port, loginName, password, true);
    }

    public void updateMgrFile(byte[] mgrFile, String mgrFileName) throws Exception {
        InputStream is = null;

        try {
            is = new ByteArrayInputStream(mgrFile);

            putResource("uploadMgrFile/" + mgrFileName, is);
        } finally {
            IOUtils.closeQuietly(is);
        }
    }
}
