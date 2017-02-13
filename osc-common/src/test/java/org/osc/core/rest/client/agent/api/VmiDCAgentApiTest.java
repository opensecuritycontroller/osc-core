package org.osc.core.rest.client.agent.api;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.Before;
import org.junit.Test;
import org.osc.core.rest.client.agent.api.VmidcAgentApi;
import org.osc.core.rest.client.agent.api.test.util.AgentApiMock;
import org.osc.core.rest.client.agent.model.output.AgentStatusResponse;

import javax.ws.rs.core.Application;

import java.io.File;
import java.nio.file.Files;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * Created by GER\bsulich on 1/10/17.
 */
public class VmiDCAgentApiTest extends JerseyTest {

    private VmidcAgentApi vmidcAgentApi;

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
        this.vmidcAgentApi = new VmidcAgentApi(this.localhost, this.port, login,
                password, false);
    }

    @Test
    public void testClientAgentApiGetStatus() {
        // Arrange.
        AgentStatusResponse response = null;

        // Act.
        try {
            response = this.vmidcAgentApi.getStatus();
        } catch (Exception e) {
            fail("VmiDCAgentApi test fails with exception: " + e);
        }

        // Assert.
        assertNotNull("Response and expectedResponse should match", response);
    }

    @Test
    public void testClientAgentApiGetFullStatus() {
        // Arrange.
        AgentStatusResponse response = null;


        // Act.
        try {
            response = this.vmidcAgentApi.getFullStatus();
        } catch (Exception e) {
            fail("VmiDCAgentApi test fails with exception: " + e);
        }

        // Assert.
        assertNotNull("Response and expectedResponse should match", response);

        assertNotNull("Response Version and expectedResponse Version should match", response.getVersion());
    }

    @Test
    public void testClientAgentApiGetSupportBundle() {
        // Arrange.
        File response = null;

        // Act.
        try {
            response = this.vmidcAgentApi.downloadLogFile();
        } catch (Exception e) {
            fail("VmiDCAgentApi test fails with exception: " + e);
        }

        // Assert.
        assertNotNull("Response and expectedResponse should match", response);

        assertNotNull("Response Version and expectedResponse Version should match", response.getAbsoluteFile());
    }

}