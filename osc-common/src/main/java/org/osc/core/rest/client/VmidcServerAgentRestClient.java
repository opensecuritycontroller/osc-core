package org.osc.core.rest.client;

import javax.ws.rs.core.MediaType;

public class VmidcServerAgentRestClient extends RestBaseClient {

    private static final String VMIDC_SERVER_REST_URL_BASE = "/api/agent/v1";

    public VmidcServerAgentRestClient(String vmiDCServer, String loginName, String password) {

        super(VMIDC_SERVER_REST_URL_BASE, MediaType.APPLICATION_JSON);

        // If IP address does include port, we'll default.
        if (!vmiDCServer.contains(":")) {
            vmiDCServer += ":8090";
        }

        initRestBaseClient(vmiDCServer, 0, loginName, password, true, true);
    }
}
