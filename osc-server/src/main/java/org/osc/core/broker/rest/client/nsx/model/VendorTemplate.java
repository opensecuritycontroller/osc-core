package org.osc.core.broker.rest.client.nsx.model;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class VendorTemplate {

    private String id;
    private String name;
    private String description;
    private String idFromVendor;

    @XmlElement(name = "vendorAttributes")
    private List<Attribute> vendorAttributes;

    public List<Attribute> getVendorAttributes() {
        return vendorAttributes;
    }

    public void setVendorAttributes(List<Attribute> vendorAttributes) {
        this.vendorAttributes = vendorAttributes;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    @Override
    public String toString() {
        return "Id [id=" + id + ", name=" + name + ", description=" + description + ", idFromVendor=" + idFromVendor
                + ", vendorAttributes=" + vendorAttributes + "]";
    }

}
