package org.osc.core.rest.client.agent.model.output;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.osc.core.rest.client.annotations.VmidcLogHidden;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class AgentCurrentVmidcPasswordResponse {

    @VmidcLogHidden
    private String vmidcServerPassword;

    public String getVmidcServerPassword() {
        return this.vmidcServerPassword;
    }

    public void setVmidcServerPassword(String vmidcServerPassword) {
        this.vmidcServerPassword = vmidcServerPassword;
    }

    @Override
    public String toString() {
        return "AgentCurrentVmidcPasswordResponse [vmidcServerPassword= ***hidden*** ]";
    }
}
