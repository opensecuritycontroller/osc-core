/*******************************************************************************
 * Copyright (c) Intel Corporation
 * Copyright (c) 2017
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.osc.core.broker.service.request;

import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.osc.core.broker.service.request.ServiceProfileInput.ProfileAttributes;
import org.osc.core.broker.service.request.ServiceProfileReference.ServiceInstanceReference;
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
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.description == null) ? 0 : this.description.hashCode());
        result = prime * result + ((this.name == null) ? 0 : this.name.hashCode());
        result = prime * result + ((this.objectId == null) ? 0 : this.objectId.hashCode());
        result = prime * result + ((this.status == null) ? 0 : this.status.hashCode());
        result = prime * result + ((this.vsmUuid == null) ? 0 : this.vsmUuid.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ServiceProfile other = (ServiceProfile) obj;
        if (this.description == null) {
            if (other.description != null) {
                return false;
            }
        } else if (!this.description.equals(other.description)) {
            return false;
        }
        if (this.name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!this.name.equals(other.name)) {
            return false;
        }
        if (this.objectId == null) {
            if (other.objectId != null) {
                return false;
            }
        } else if (!this.objectId.equals(other.objectId)) {
            return false;
        }
        if (this.status == null) {
            if (other.status != null) {
                return false;
            }
        } else if (!this.status.equals(other.status)) {
            return false;
        }
        if (this.vsmUuid == null) {
            if (other.vsmUuid != null) {
                return false;
            }
        } else if (!this.vsmUuid.equals(other.vsmUuid)) {
            return false;
        }
        return true;
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
