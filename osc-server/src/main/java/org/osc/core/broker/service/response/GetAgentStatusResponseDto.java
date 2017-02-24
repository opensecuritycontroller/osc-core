package org.osc.core.broker.service.response;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.osc.core.rest.client.agent.model.output.AgentStatusResponse;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class GetAgentStatusResponseDto implements Response {
    private List<AgentStatusResponse> agentStatusList = null;

    public List<AgentStatusResponse> getAgentStatusList() {
        return this.agentStatusList;
    }

    public void setAgentStatusList(List<AgentStatusResponse> agentStatusList) {
        this.agentStatusList = agentStatusList;
    }

    @Override
    public String toString() {
        return "GetAgentStatusResponseDto [agentStatusDtoList=" + this.agentStatusList + "]";
    }
}
