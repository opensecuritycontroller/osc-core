package org.osc.core.broker.rest.client.nsx.model;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "serviceInstanceAttributes")
@XmlAccessorType(XmlAccessType.FIELD)
public class ServiceInstanceAttributes {

    @XmlElement(name = "attribute")
    public List<Attribute> attribute = new ArrayList<Attribute>();

    public List<Attribute> getAttribute() {
        return this.attribute;
    }

    public void setAttribute(List<Attribute> attribute) {
        this.attribute = attribute;
    }

    @Override
    public String toString() {
        return "ServiceInstanceAttributes [attribute=" + this.attribute + "]";
    }

}
