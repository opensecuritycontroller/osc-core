package org.osc.core.rest.client.agent.model.input;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.osc.core.rest.client.agent.model.input.dpaipc.InterfaceEndpointMap;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class AgentSetInterfaceEndpointMapRequest {

    private InterfaceEndpointMap interfaceEndpointMap;

    public InterfaceEndpointMap getInterfaceEndpointMap() {
        return interfaceEndpointMap;
    }

    public void setInterfaceEndpointMap(InterfaceEndpointMap interfaceEndpointMap) {
        this.interfaceEndpointMap = interfaceEndpointMap;
    }

    @Override
    public String toString() {
        return "AgentSetInterfaceEndpointMapRequest [interfaceEndpointMap=" + interfaceEndpointMap + "]";
    }

}
