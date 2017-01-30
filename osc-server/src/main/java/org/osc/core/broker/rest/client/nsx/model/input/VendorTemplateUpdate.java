package org.osc.core.broker.rest.client.nsx.model.input;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "vendorTemplate")
@XmlAccessorType(XmlAccessType.FIELD)
public class VendorTemplateUpdate {
    private String id;
    private String name;
    private String description;
    private String idFromVendor;

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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "VendorTemplateUpdate [id=" + id + ", name=" + name + ", description=" + description + ", idFromVendor="
                + idFromVendor + "]";
    }
}
