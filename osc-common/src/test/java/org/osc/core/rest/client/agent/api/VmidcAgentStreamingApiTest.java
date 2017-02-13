package org.osc.core.rest.client.agent.api;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.Before;
import org.junit.Test;
import org.osc.core.rest.client.agent.api.VmidcAgentStreamingApi;
import org.osc.core.rest.client.agent.api.test.util.AgentApiMock;

import javax.ws.rs.core.Application;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * RestClientTest streamingTest.
 */
public class VmidcAgentStreamingApiTest extends JerseyTest {

    private VmidcAgentStreamingApi vmidcAgentStreamingApi;

    private String localhost = "localhost";

    private int port = 9998;

    private String login = "login";

    private String password = "password";

    @Override
    protected Application configure() {
        return new ResourceConfig()
                .register(AgentApiMock.class);
    }

    @Before
    public void prepareRestEndpoint() throws Exception {
        this.vmidcAgentStreamingApi = new VmidcAgentStreamingApi(this.localhost, this.port, login,
                password, false);
    }

    @Test
    public void testClientAgentApiUpdateMgrFile() {
        // Arrange.
        byte[] bytes = "fileContent".getBytes();

        String file = "file";

        // Act
        try {
            this.vmidcAgentStreamingApi.updateMgrFile(bytes, file);
        } catch (Exception e) {
            fail("VmidcAgentStreamingApi test fails with exception: " + e);
        }

        //Assert
        assertTrue(true);
    }


}