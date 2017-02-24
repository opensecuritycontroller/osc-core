package org.osc.core.broker.service.request;

import org.osc.core.broker.rest.client.nsx.model.FabricAgents;

public class NsxUpdateAgentsRequest implements Request {
    public String nsxIpAddress;
    public FabricAgents fabricAgents;
}
