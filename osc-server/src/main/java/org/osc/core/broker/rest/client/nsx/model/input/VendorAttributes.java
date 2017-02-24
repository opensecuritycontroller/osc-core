package org.osc.core.broker.rest.client.nsx.model.input;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.osc.core.broker.rest.client.nsx.model.Attribute;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class VendorAttributes {

    @XmlElement
    public List<Attribute> attribute = new ArrayList<Attribute>();

    public List<Attribute> getAttribute() {
        return attribute;
    }

    public void setAttribute(List<Attribute> attribute) {
        this.attribute = attribute;
    }

}
