package org.osc.core.broker.rest.client.nsx.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "config")
@XmlAccessorType(XmlAccessType.FIELD)
public class ServiceInstanceConfig {
    private String id;
    private String revision;
    private ServiceInstanceAttributes serviceInstanceAttributes;
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public String getRevision() {
        return revision;
    }
    public void setRevision(String revision) {
        this.revision = revision;
    }
    public ServiceInstanceAttributes getServiceInstanceAttributes() {
        return serviceInstanceAttributes;
    }
    public void setServiceInstanceAttributes(ServiceInstanceAttributes serviceInstanceAttributes) {
        this.serviceInstanceAttributes = serviceInstanceAttributes;
    }
    @Override
    public String toString() {
        return "ServiceInstanceConfig [id=" + id + ", revision=" + revision + ", serviceInstanceAttributes="
                + serviceInstanceAttributes + "]";
    } 
    
}
