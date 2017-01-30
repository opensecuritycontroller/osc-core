package org.osc.core.broker.rest.client.nsx.model;

import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.osc.core.broker.rest.client.nsx.model.ServiceProfileReference.ServiceInstanceReference;
import org.osc.core.broker.rest.client.nsx.model.input.ServiceProfileInput.ProfileAttributes;
import org.osc.sdk.sdn.element.ServiceProfileElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class ServiceProfile implements ServiceProfileElement {
    private String objectId;
    public String vsmUuid;
    private String name;
    public String description;
    public String status;

    private Service service;
    private ServiceInstanceReference serviceInstance;
    private VendorTemplate vendorTemplate;
    public ProfileAttributes profileAttributes = new ProfileAttributes();
    public ServiceProfileBinding serviceProfileBinding = new ServiceProfileBinding();

    public Service getService() {
        return this.service;
    }

    public void setService(Service service) {
        this.service = service;
    }

    public ServiceInstanceReference getServiceInstance() {
        return this.serviceInstance;
    }

    public void setServiceInstance(ServiceInstanceReference serviceInstance) {
        this.serviceInstance = serviceInstance;
    }

    public String getServiceProfileId() {
        return this.objectId;
    }

    public void setServiceProfileId(String serviceProfileId) {
        this.objectId = serviceProfileId;
    }

    @Override
    public String toString() {
        return "ServiceProfile [objectId=" + this.objectId + ", vsmUuid=" + this.vsmUuid + ", name=" + this.name + ", description="
                + this.description + ", status=" + this.status + ", service=" + this.service + ", serviceInstance=" + this.serviceInstance
                + ", vendorTemplate=" + this.vendorTemplate + ", profileAttributes=" + this.profileAttributes
                + ", serviceProfileBinding=" + this.serviceProfileBinding + "]";
    }

    @Override
    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public VendorTemplate getVendorTemplate() {
        return this.vendorTemplate;
    }

    public void setVendorTemplate(VendorTemplate vendorTemplate) {
        this.vendorTemplate = vendorTemplate;
    }

    public String getObjectId() {
        return this.objectId;
    }

    public void setObjectId(String objectId) {
        this.objectId = objectId;
    }

    public String getVsmUuid() {
        return this.vsmUuid;
    }

    public void setVsmUuid(String vsmUuid) {
        this.vsmUuid = vsmUuid;
    }

    @Override
    public boolean equals(Object object) {
        if (object == null) {
            return false;
        }
        if (getClass() != object.getClass()) {
            return false;
        }
        if (this == object) {
            return true;
        }

        ServiceProfile other = (ServiceProfile) object;

        return new EqualsBuilder()
                .append(getName(), other.getName())
                .append(getObjectId(), other.getObjectId())
                .append(getVsmUuid(), other.getVsmUuid())
                .append(this.description, other.description)
                .append(this.status, other.status)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(getName())
                .append(getObjectId())
                .append(getVsmUuid())
                .append(this.description)
                .append(this.status)
                .toHashCode();
    }

    @Override
    public String getId() {
        return this.objectId;
    }

    @Override
    public String getVsmId() {
        return this.vsmUuid;
    }

    @Override
    public String getServiceInstanceId() {
        return this.serviceInstance == null ? null : this.serviceInstance.objectId;
    }

    @Override
    public String getServiceInstanceName() {
        return this.serviceInstance == null ? null : this.serviceInstance.name;
    }

    @Override
    public String getServiceInstanceVsmId() {
        return this.serviceInstance == null ? null : this.serviceInstance.vsmUuid;
    }

    @Override
    public String getVendorTemplateId() {
        return this.vendorTemplate == null ? null : this.vendorTemplate.getId();
    }

    @Override
    public Set<String> getDistributedVirtualPortGroups() {
        return this.serviceProfileBinding.distributedVirtualPortGroups.string;
    }

    @Override
    public Set<String> getSecurityGroups() {
        return this.serviceProfileBinding.securityGroups.string;
    }

    @Override
    public Set<String> getVirtualServers() {
        return this.serviceProfileBinding.virtualServers.string;
    }

    @Override
    public Set<String> getVirtualWires() {
        return this.serviceProfileBinding.virtualWires.string;
    }
}
