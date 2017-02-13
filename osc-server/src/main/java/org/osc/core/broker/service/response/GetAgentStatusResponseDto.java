package org.osc.core.broker.service.response;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class GetAgentStatusResponseDto implements Response {
    private List<AgentStatusResponseDto> agentStatusDtoList = new ArrayList<>();

    public List<AgentStatusResponseDto> getAgentStatusDtoList() {
        return this.agentStatusDtoList;
    }

    public void setAgentStatusList(List<AgentStatusResponseDto> agentStatusDtoList) {
        this.agentStatusDtoList = agentStatusDtoList;
    }

    @Override
    public String toString() {
        return "GetAgentStatusResponseDto [agentStatusDtoList=" + this.agentStatusDtoList + "]";
    }

}
