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

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class ServiceProfiles {

    @XmlElement(name = "serviceProfile")
    public List<ServiceProfile> list = new ArrayList<ServiceProfile>();

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (ServiceProfile sp : this.list) {
            sb.append(sp.toString() + "\n");
        }
        return sb.toString();
    }

    public ServiceProfile findServiceProfileByService(Service sv) {
        for (ServiceProfile sp : this.list) {
            if (sp.getService().getId().equals(sv.getId())) {
                return sp;
            }
        }
        return null;
    }
}
