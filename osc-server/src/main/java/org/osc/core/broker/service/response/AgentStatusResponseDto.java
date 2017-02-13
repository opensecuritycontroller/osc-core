package org.osc.core.broker.service.response;

import org.osc.core.rest.client.agent.model.output.AgentStatusResponse;

public class AgentStatusResponseDto {
    private AgentStatusResponse response;
    private String agentType;

    public AgentStatusResponseDto() {
    }

    public AgentStatusResponseDto(AgentStatusResponse response, String agentType) {
        this.response = response;
        this.agentType = agentType;
    }
    public AgentStatusResponse getResponse() {
        return this.response;
    }
    public void setResponse(AgentStatusResponse response) {
        this.response = response;
    }
    public String getAgentType() {
        return this.agentType;
    }
    public void setAgentType(String agentType) {
        this.agentType = agentType;
    }

}
