package org.osc.core.broker.rest.client.nsx.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class ServiceProfileReference {
    public String objectId;
    public String name;
    public ServiceReference service;
    public ServiceInstanceReference serviceInstance;
    
    @XmlRootElement(name = "service")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class ServiceReference {

        public String objectId;
        public String name;

        @Override
        public String toString() {
            return "ServiceReference [objectId=" + objectId + ", name=" + name + "]";
        }

    }
    
    @XmlRootElement(name = "serviceInstance")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class ServiceInstanceReference {

        public String objectId;
        public String name;
        public String vsmUuid;

        @Override
        public String toString() {
            return "ServiceReference [objectId=" + objectId + ", name=" + name + "]";
        }

    }

    public ServiceProfileReference() {
    }
    public ServiceProfileReference(String objectId) {
        this.objectId = objectId;
    }
    
    @Override
    public String toString() {
        return "ServiceProfileReference [objectId=" + objectId + ", name=" + name + ", service=" + service
                + ", serviceInstance=" + serviceInstance + "]";
    }

}
