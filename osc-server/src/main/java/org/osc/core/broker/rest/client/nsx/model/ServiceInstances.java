/*******************************************************************************
 * Copyright (c) 2017 Intel Corporation
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

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class ServiceInstances {

    @XmlElement(name = "serviceInstance")
    public List<ServiceInstance> list = new ArrayList<ServiceInstance>();

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (ServiceInstance si : this.list) {
            sb.append(si.toString() + "\n");
        }
        return sb.toString();
    }

    public ServiceInstance findServiceInstanceByService(Service sv) {
        for (ServiceInstance si : this.list) {
            if (si.getService().objectId.equals(sv.getId())) {
                return si;
            }
        }
        return null;
    }
}
