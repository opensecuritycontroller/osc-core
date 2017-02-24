package org.osc.core.broker.rest.client.nsx.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class AgentHealthStatus {
    public String status;
    public String errorId;
    public String errorDescription;
}
