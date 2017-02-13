package org.osc.core.rest.client;

import javax.ws.rs.core.MediaType;

public class VmidcServerRestClient extends RestBaseClient {

    private static final String VMIDC_SERVER_REST_URL_BASE = "/api/server/v1/serverManagement";
    private static final String VMIDC_SERVER_REST_DEBUG_URL_BASE = "/api/server/v1/serverDebug";

    /**
     * Server debug rest client
     */
    public VmidcServerRestClient(int port) {

        super(VMIDC_SERVER_REST_DEBUG_URL_BASE, MediaType.TEXT_PLAIN);

        initRestBaseClient("localhost", port, null, null, true);
    }

    /**
     * Server Management rest client
     */
    public VmidcServerRestClient(String vmiDCServer, int port, String loginName, String password, boolean isHttps) {

        super(VMIDC_SERVER_REST_URL_BASE, MediaType.APPLICATION_JSON);

        initRestBaseClient(vmiDCServer, port, loginName, password, isHttps);
    }

}
