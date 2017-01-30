package org.osc.core.broker.rest.client.nsx.model.input;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "vendorTemplate")
@XmlAccessorType(XmlAccessType.FIELD)
public class VendorTemplateInput {
    private String name;
    private String description;
    private String idFromVendor;

    @XmlElement(name = "vendorAttributes")
    private VendorAttributes vendorAttributes;

    public VendorAttributes getVendorAttributes() {
        return vendorAttributes;
    }

    public void setServiceAttributes(VendorAttributes vendorAttributes) {
        this.vendorAttributes = vendorAttributes;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIdFromVendor() {
        return idFromVendor;
    }

    public void setIdFromVendor(String idFromVendor) {
        this.idFromVendor = idFromVendor;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
