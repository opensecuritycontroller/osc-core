package org.osc.core.broker.rest.client.nsx.model.input;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.osc.core.broker.rest.client.nsx.model.Attribute;
import org.osc.core.broker.rest.client.nsx.model.ServiceProfile;

@XmlRootElement(name = "serviceProfile")
@XmlAccessorType(XmlAccessType.FIELD)
public class ServiceProfileInput {

    @XmlRootElement
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class ObjectId {
        private String objectId;

        public ObjectId() {

        }

        public ObjectId(String objectId) {
            this.objectId = objectId;
        }

        public String getObjectId() {
            return this.objectId;
        }

        public void setObjectId(String objectId) {
            this.objectId = objectId;
        }
    }

    @XmlRootElement
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Id {
        private String id;

        public Id() {

        }

        public Id(String id) {
            this.id = id;
        }

        public String getId() {
            return this.id;
        }

        public void setObjectId(String id) {
            this.id = id;
        }
    }

    @XmlRootElement
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class ProfileAttributes {

        @XmlElement
        private List<Attribute> attribute = new ArrayList<Attribute>();

        public List<Attribute> getAttribute() {
            return this.attribute;
        }

        public void setAttribute(List<Attribute> attribute) {
            this.attribute = attribute;
        }
    }

    private String name;
    private String description;
    private ObjectId serviceInstance = new ObjectId();
    private ObjectId service = new ObjectId();
    private Id vendorTemplate = new Id();
    private ProfileAttributes profileAttributes = new ProfileAttributes();

    public ServiceProfileInput() {
    }

    public ServiceProfileInput(ServiceProfile sp) {
        this.name = sp.getName();
        this.description = sp.description;
        this.service.objectId = sp.getService().getId();
        this.serviceInstance.objectId = sp.getServiceInstance().objectId;
        this.vendorTemplate.id = sp.getVendorTemplate().getId();
        this.profileAttributes = sp.profileAttributes;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ObjectId getServiceInstance() {
        return this.serviceInstance;
    }

    public void setServiceInstance(ObjectId serviceInstance) {
        this.serviceInstance = serviceInstance;
    }

    public Id getVendorTemplate() {
        return this.vendorTemplate;
    }

    public void setVendorTemplate(Id vendorTemplate) {
        this.vendorTemplate = vendorTemplate;
    }

    public ProfileAttributes getProfileAttributes() {
        return this.profileAttributes;
    }

    public void setProfileAttributes(ProfileAttributes profileAttributes) {
        this.profileAttributes = profileAttributes;
    }

    public ObjectId getService() {
        return this.service;
    }

    public void setService(ObjectId service) {
        this.service = service;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

}
