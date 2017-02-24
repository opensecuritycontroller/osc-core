package org.osc.core.broker.rest.client.nsx.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "serviceInstance")
@XmlAccessorType(XmlAccessType.FIELD)
public class ServiceInstanceDetails {
    
    private String objectId;
    protected String vsmUuid;
    private String name;
    private ServiceInstanceConfig config;
    private ServiceReference service;
    
    @XmlRootElement(name = "service")
    @XmlAccessorType(XmlAccessType.FIELD)
    private static class ServiceReference {
        private String objectId;
    
        @Override
        public String toString() {
            return "ServiceReference [objectId=" + objectId + "]";
        }
    }
    
    public void setServiceObjectId(String objId) {
        service.objectId = objId;
    }
    
    public String getServiceObjectId() {
        return service.objectId;
    }
    
    public ServiceReference getService() {
        return service;
    }
    public void setService(ServiceReference service) {
        this.service = service;
    }
    public String getObjectId() {
        return objectId;
    }
    public void setObjectId(String objectId) {
        this.objectId = objectId;
    }
    public String getVsmUuid() {
        return vsmUuid;
    }
    public void setVsmUuid(String vsmUuid) {
        this.vsmUuid = vsmUuid;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public ServiceInstanceConfig getConfig() {
        return config;
    }
    public void setConfig(ServiceInstanceConfig config) {
        this.config = config;
    }
    
    @Override
    public String toString() {
        return "ServiceInstanceDetails [objectId=" + objectId + ", vsmUuid=" + vsmUuid + ", name=" + name + ", config="
                + config + ", service=" + service + "]";
    }
}
