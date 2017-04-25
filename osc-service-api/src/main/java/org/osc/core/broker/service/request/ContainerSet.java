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

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.osc.core.broker.service.request.EndpointGroupList.EndpointGroup;
import org.osc.sdk.sdn.element.SecurityGroupElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class ContainerSet {

    @XmlElement(name = "container")
    private List<Container> list = new ArrayList<Container>();

    public ContainerSet() {
    }

    public ContainerSet(List<SecurityGroupElement> securityGroups) {
        if (securityGroups == null) {
            throw new IllegalArgumentException("The securityGroups cannot be null.");
        }

        for (SecurityGroupElement securityGroup : securityGroups) {
            this.list.add(new Container(securityGroup));
        }
    }

    @XmlRootElement
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Container {
        protected String id;
        protected String name;
        protected String description;
        protected String type; // IP or MAC
        protected List<String> address = new ArrayList<String>(); // ip3 or mac3

        public Container() {
        }

        public Container(SecurityGroupElement securityGroup) {
            if (securityGroup == null) {
                throw new IllegalArgumentException("The securityGroup cannot be null.");
            }

            this.id = securityGroup.getId();
            this.name = securityGroup.getName();
            this.type = securityGroup.getType();
            this.address = securityGroup.getAddressess();
        }

        public String getId() {
            return this.id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return this.name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getType() {
            return this.type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public List<String> getAddress() {
            return this.address;
        }

        public void setAddress(List<String> address) {
            this.address = address;
        }

        @Override
        public String toString() {
            return "Container [id=" + this.id + ", name=" + this.name + ", type=" + this.type + ", address=" + this.address + "]";
        }

        public EndpointGroup toIscEndpointGroup() {
            EndpointGroup endpointGroup = new EndpointGroup();
            endpointGroup.id = getId();
            endpointGroup.name = getName();
            endpointGroup.type = getType();
            endpointGroup.addresses = getAddress();
            return endpointGroup;
        }

    }

    public List<Container> getList() {
        return this.list;
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        for (Container container : this.list) {
            sb.append(container.toString() + "\n");
        }
        return sb.toString();
    }

    public EndpointGroupList toIscEndpointGroupSet() {
        EndpointGroupList cs = new EndpointGroupList();
        for (Container container : this.list) {
            EndpointGroupList.EndpointGroup c = container.toIscEndpointGroup();
            cs.endpointGroups.add(c);
        }
        return cs;
    }

}
