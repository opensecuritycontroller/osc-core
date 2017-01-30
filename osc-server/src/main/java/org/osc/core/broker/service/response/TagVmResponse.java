package org.osc.core.broker.service.response;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "tagVmResponse")
@XmlAccessorType(XmlAccessType.FIELD)
public class TagVmResponse implements Response {

    @XmlElement(name = "vmTag")
    public String vmTag;

    public String getVmTag() {
        return vmTag;
    }

    public void setVmTag(String vmTag) {
        this.vmTag = vmTag;
    }

    @Override
    public String toString() {
        return "TagVmResponse [vmTag=" + vmTag + "]";
    }
}

