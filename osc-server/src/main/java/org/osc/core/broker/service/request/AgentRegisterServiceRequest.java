package org.osc.core.broker.service.request;

import org.osc.core.rest.client.agent.model.input.AgentRegisterRequest;

public class AgentRegisterServiceRequest extends AgentRegisterRequest implements Request {

    public AgentRegisterServiceRequest() {

    }

    public AgentRegisterServiceRequest(AgentRegisterRequest request) {
        setAgentVersion(request.getAgentVersion());
        setApplianceIp(request.getApplianceIp());
        setApplianceGateway(request.getApplianceGateway());
        setApplianceSubnetMask(request.getApplianceSubnetMask());
        setName(request.getName());
        setVirtualSystemId(request.getVsId());

        setDiscovered(request.isDiscovered());
        setInspectionReady(request.isInspectionReady());

        setCpaPid(request.getCpaPid());
        setCurrentServerTime(request.getCurrentServerTime());
        setCpaUptime(request.getCpaUptime());

        setAgentDpaInfo(request.getAgentDpaInfo());
    }

}
