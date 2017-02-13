package org.osc.core.rest.client.agent.api;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import javax.ws.rs.core.MediaType;

import org.apache.commons.io.IOUtils;
import org.osc.core.rest.client.RestBaseClient;

public class VmidcAgentStreamingApi extends RestBaseClient {

    private static final String VMIDC_SERVER_REST_URL_BASE = "/api/agent/v1";

    public VmidcAgentStreamingApi(String agentServer, int port, String loginName, String password) {
        this(agentServer, port, loginName, password, true);
    }

    protected VmidcAgentStreamingApi(String agentServer, int port, String loginName, String password, boolean isHttps) {

        super(VMIDC_SERVER_REST_URL_BASE, MediaType.APPLICATION_OCTET_STREAM);

        initRestBaseClient(agentServer, port, loginName, password, isHttps);
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
