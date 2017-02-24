package org.osc.core.rest.client.agent.model.input;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class AgentUpdateVmidcServerRequest {

    private String vmidcServerIp;

    public String getVmidcServerIp() {
        return vmidcServerIp;
    }

    public void setVmidcServerIp(String vmidcServerIp) {
        this.vmidcServerIp = vmidcServerIp;
    }

    @Override
    public String toString() {
        return "AgentUpdateVmidcServerRequest [vmidcServerIp=" + vmidcServerIp + "]";
    }

}
