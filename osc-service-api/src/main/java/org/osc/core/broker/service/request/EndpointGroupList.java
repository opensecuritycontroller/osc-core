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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@SuppressWarnings("serial")
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class EndpointGroupList implements Serializable {

    @XmlElement
    public List<EndpointGroup> endpointGroups = new ArrayList<EndpointGroup>();

    @XmlRootElement
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class EndpointGroup implements Serializable{
        public String id;
        public String name;
        public String type; // IP or MAC
        public List<String> addresses = new ArrayList<String>();

        @Override
        public String toString() {
            return "Endpoints [id=" + id + ", name=" + name + ", type=" + type + ", address=" + addresses + "]";
        }

    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (EndpointGroup container : endpointGroups) {
            sb.append(container.toString() + "\n");
        }
        return sb.toString();
    }

}
