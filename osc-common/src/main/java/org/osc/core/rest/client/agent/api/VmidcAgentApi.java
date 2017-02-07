package org.osc.core.rest.client.agent.api;

import org.osc.core.rest.client.RestBaseClient;
import org.osc.core.rest.client.agent.model.input.AgentSetInterfaceEndpointMapRequest;
import org.osc.core.rest.client.agent.model.input.AgentUpdateConsolePasswordRequest;
import org.osc.core.rest.client.agent.model.input.AgentUpdateInterfaceEndpointMapRequest;
import org.osc.core.rest.client.agent.model.input.AgentUpdateVmidcPasswordRequest;
import org.osc.core.rest.client.agent.model.input.AgentUpdateVmidcServerRequest;
import org.osc.core.rest.client.agent.model.input.AgentUpgradeRequest;
import org.osc.core.rest.client.agent.model.input.EndpointGroupList;
import org.osc.core.rest.client.agent.model.input.dpaipc.InterfaceEndpointMap;
import org.osc.core.rest.client.agent.model.output.AgentStatusResponse;
import org.osc.core.rest.client.agent.model.output.AgentSupportBundle;
import org.osc.core.rest.client.agent.model.output.AgentUpgradeResponse;
import org.osc.core.util.PKIUtil;

import javax.ws.rs.core.MediaType;
import java.io.File;
import java.net.URI;

public class VmidcAgentApi extends RestBaseClient {

    private static final String VMIDC_SERVER_REST_URL_BASE = "/api/agent/v1";

    public VmidcAgentApi(String agentServer, int port, String loginName, String password)
            throws Exception {
        this(agentServer, port, loginName, password,true,true);
    }

    protected VmidcAgentApi(String agentServer, int port, String loginName, String password, boolean isHttps)
            throws Exception {
        super(VMIDC_SERVER_REST_URL_BASE, MediaType.APPLICATION_JSON);

        initRestBaseClient(agentServer, port, loginName, password, isHttps, true);
    }

    public File downloadLogFile() throws Exception {
        AgentSupportBundle res = getResource("getSupportBundle", AgentSupportBundle.class);
        PKIUtil.writeBytesToFile(res.getSupportLogBundle(), ".", "AgentSupportBundle.zip");
        return new File("AgentSupportBundle.zip");
    }

    public AgentStatusResponse getStatus() throws Exception {
        return getResource("status", AgentStatusResponse.class);
    }

    public AgentStatusResponse getFullStatus() throws Exception {
        return getResource("fullstatus", AgentStatusResponse.class);
    }

    public String register() throws Exception {
        return postResource("register", String.class, null);
    }

    public void upgrade(String upgradePackageUrl) throws Exception {
        AgentUpgradeRequest agentUpgrade = new AgentUpgradeRequest();
        agentUpgrade.setUpgradePackageUrl(URI.create(upgradePackageUrl).toURL());
        postResource("upgrade", AgentUpgradeResponse.class, agentUpgrade);
    }

    public void updateVmidcServerPassowrd(String newPassword) throws Exception {
        AgentUpdateVmidcPasswordRequest agentUpdateVmidcPasswordRequest = new AgentUpdateVmidcPasswordRequest();
        agentUpdateVmidcPasswordRequest.setVmidcServerPassword(newPassword);
        putResource("update-agent-vmidcserver-password", agentUpdateVmidcPasswordRequest);
    }

    public void updateVmidcServerIp(String hostIpAddress) throws Exception {
        AgentUpdateVmidcServerRequest agentUpdateVmidcServerRequest = new AgentUpdateVmidcServerRequest();
        agentUpdateVmidcServerRequest.setVmidcServerIp(hostIpAddress);
        putResource("update-agent-vmidcserver-ip", agentUpdateVmidcServerRequest);
    }

    public void updateInterfaceEndpointMap(String interfaceTag, EndpointGroupList endpoints) throws Exception {
        AgentUpdateInterfaceEndpointMapRequest req = new AgentUpdateInterfaceEndpointMapRequest();
        req.interfaceTag = interfaceTag;
        req.endpoints = endpoints;
        putResource("update-interface-endpoint-map", req);
    }

    public void setInterfaceEndpointMap(InterfaceEndpointMap interfaceEndpointMap) throws Exception {
        AgentSetInterfaceEndpointMapRequest req = new AgentSetInterfaceEndpointMapRequest();
        req.setInterfaceEndpointMap(interfaceEndpointMap);
        putResource("set-interface-endpoint-map", req);
    }

    public void updateApplianceConsolePassowrd(String oldPassword, String newPassword) throws Exception {
        AgentUpdateConsolePasswordRequest agentUpdateConsolePasswordRequest = new AgentUpdateConsolePasswordRequest();
        agentUpdateConsolePasswordRequest.setOldPassword(oldPassword);
        agentUpdateConsolePasswordRequest.setNewPassword(newPassword);
        putResource("update-console-password", agentUpdateConsolePasswordRequest);
    }

}
