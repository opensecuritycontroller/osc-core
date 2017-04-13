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
