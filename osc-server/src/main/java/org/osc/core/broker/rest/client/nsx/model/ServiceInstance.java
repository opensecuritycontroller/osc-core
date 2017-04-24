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

import org.osc.core.broker.service.request.ServiceProfileReference.ServiceReference;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class ServiceInstance {
    
    private String objectId;
    protected String vsmUuid;
    private String name;
    private String description;
    private ServiceReference service;
    private RuntimeInfos runtimeInfos;
    private byte serviceProfileCount;
    
    public ServiceInstance() {
    }

    public ServiceInstance(ServiceReference service) {
        this.service = service;
    }

    public String getServiceInstanceId() {
        return objectId;
    }

    public void setServiceInstanceId(String serviceInstanceId) {
        this.objectId = serviceInstanceId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ServiceReference getService() {
        return service;
    }

    public void setService(ServiceReference service) {
        this.service = service;
    }

    public RuntimeInfos getRuntimeInfos() {
        return runtimeInfos;
    }

    public void setRuntimeInfos(RuntimeInfos runtimeInfos) {
        this.runtimeInfos = runtimeInfos;
    }

    public byte getServiceProfileCount() {
        return serviceProfileCount;
    }

    public void setServiceProfileCount(byte serviceProfileCount) {
        this.serviceProfileCount = serviceProfileCount;
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

    @Override
    public String toString() {
        return "ServiceInstance [objectId=" + objectId + ", name=" + name + "]";
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

}
