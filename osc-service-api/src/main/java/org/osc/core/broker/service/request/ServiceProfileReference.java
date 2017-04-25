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
