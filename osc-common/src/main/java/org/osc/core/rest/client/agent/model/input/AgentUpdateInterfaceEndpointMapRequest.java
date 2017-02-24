package org.osc.core.rest.client.agent.model.input;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class AgentUpdateInterfaceEndpointMapRequest {

    public String interfaceTag;
    public EndpointGroupList endpoints;

    @Override
    public String toString() {
        return "AgentUpdateInterfaceEndpointMapRequest [interfaceTag=" + interfaceTag + ", endpoints=" + endpoints
                + "]";
    }

}
