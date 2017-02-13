package org.osc.core.rest.client;

import javax.ws.rs.core.MediaType;

public class VmidcAgentServerRestClient extends RestBaseClient {

    private static final String VMIDC_SERVER_REST_URL_BASE = "/api/agent/v1";

    public VmidcAgentServerRestClient(String agentServer, int port, String loginName, String password, boolean isHttps) {

        super(VMIDC_SERVER_REST_URL_BASE, MediaType.APPLICATION_JSON);

        initRestBaseClient(agentServer, port, loginName, password, isHttps, true);
    }
}
