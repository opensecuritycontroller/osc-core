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

import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class ServiceProfileBinding {

    public static class DistributedVirtualPortGroups {
        public Set<String> string = new HashSet<String>();

        @Override
        public String toString() {
            return "DistributedVirtualPortGroups [string=" + string + "]";
        }
    }

    public static class SecurityGroups {
        public Set<String> string = new HashSet<String>();

        @Override
        public String toString() {
            return "SecurityGroups [string=" + string + "]";
        }
    }

    public static class VirtualWires {
        public Set<String> string = new HashSet<String>();

        @Override
        public String toString() {
            return "VirtualWires [string=" + string + "]";
        }
    }

    public static class VirtualServers {
        public Set<String> string = new HashSet<String>();

        @Override
        public String toString() {
            return "VirtualServers [string=" + string + "]";
        }
    }

    private ServiceProfile serviceProfile;
    private String serviceProfileBindingId;
    public DistributedVirtualPortGroups distributedVirtualPortGroups = new DistributedVirtualPortGroups();
    public SecurityGroups securityGroups = new SecurityGroups();
    public VirtualWires virtualWires = new VirtualWires();
    public VirtualServers virtualServers = new VirtualServers();

    public ServiceProfile getServiceProfile() {
        return serviceProfile;
    }

    public void setServiceProfile(ServiceProfile serviceProfile) {
        this.serviceProfile = serviceProfile;
    }

    public String getServiceProfileBindingId() {
        return serviceProfileBindingId;
    }

    public void setServiceProfileBindingId(String serviceProfileBindingId) {
        this.serviceProfileBindingId = serviceProfileBindingId;
    }

    @Override
    public String toString() {
        return "ServiceProfileBinding [serviceProfile=" + serviceProfile + ", serviceProfileBindingId="
                + serviceProfileBindingId + ", distributedVirtualPortGroups=" + distributedVirtualPortGroups + "]";
    }

}
