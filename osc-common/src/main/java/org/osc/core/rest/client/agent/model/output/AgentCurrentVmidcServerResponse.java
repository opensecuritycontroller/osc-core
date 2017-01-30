package org.osc.core.rest.client.agent.model.output;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class AgentCurrentVmidcServerResponse {

    private String vmidcServerIp;

    public String getVmidcServerIp() {
        return vmidcServerIp;
    }

    public void setVmidcServerIp(String vmidcServerIp) {
        this.vmidcServerIp = vmidcServerIp;
    }

    @Override
    public String toString() {
        return "AgentCurrentVmidcServerResponse [vmidcServerIp=" + vmidcServerIp + "]";
    }

}
