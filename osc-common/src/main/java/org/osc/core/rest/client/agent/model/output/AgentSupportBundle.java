package org.osc.core.rest.client.agent.model.output;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.osc.core.rest.client.annotations.VmidcLogHidden;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class AgentSupportBundle {
    @VmidcLogHidden
    private byte[] supportLogBundle = null;

    public byte[] getSupportLogBundle() {
        return supportLogBundle;
    }

    public void setSupportLogBundle(byte[] supportLogBundle) {
        this.supportLogBundle = supportLogBundle;
    }

}
