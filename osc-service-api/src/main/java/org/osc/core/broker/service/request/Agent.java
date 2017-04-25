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

import org.osc.sdk.sdn.element.AgentElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class Agent implements AgentElement {

    public static class ServiceStatus {
        public String status;
        public String errorId;
        public String errorDescription;

        @Override
        public String toString() {
            return "ServiceStatus [status=" + this.status + ", errorId=" + this.errorId + ", errorDescription="
                    + this.errorDescription + "]";
        }
    }

    public static class AllocatedIpAddress {
        public String id;
        public String ipAddress;
        public String gateway;
        public String prefixLength;
        public String dnsServer1;
        public String dnsServer2;

        @Override
        public String toString() {
            return "AllocatedIpAddress [id=" + this.id + ", ipAddress=" + this.ipAddress + ", gateway=" + this.gateway
                    + ", prefixLength=" + this.prefixLength + ", dnsServer1=" + this.dnsServer1 + ", dnsServer2=" + this.dnsServer2
                    + "]";
        }
    }

    public static class HostInfo {
        public String objectId;
        public String vsmUuid;
        public String name;

        @Override
        public String toString() {
            return "HostInfo [objectId=" + this.objectId + ", vsmUuid=" + this.vsmUuid + ", name=" + this.name + "]";
        }
    }

    public String agentId;
    public String agentName;
    public String vmId;
    public String host;
    public String serviceId;
    public String operationalStatus;
    public String progressStatus;
    public ServiceStatus serviceStatus;
    public AllocatedIpAddress allocatedIpAddress;
    public HostInfo hostInfo;

    @Override
    public String toString() {
        return "Agent [agentId=" + this.agentId + ", agentName=" + this.agentName + ", vmId=" + this.vmId + ", host=" + this.host
                + ", serviceId=" + this.serviceId + ", operationalStatus=" + this.operationalStatus + ", progressStatus="
                + this.progressStatus + ", serviceStatus=" + this.serviceStatus + ", allocatedIpAddress=" + this.allocatedIpAddress
                + ", hostInfo=" + this.hostInfo + "]";
    }

    @Override
    public String getId() {
        return this.agentId;
    }

    @Override
    public String getIpAddress() {
        return this.allocatedIpAddress.ipAddress;
    }

    @Override
    public String getGateway() {
        return this.allocatedIpAddress.gateway;
    }

    @Override
    public String getSubnetPrefixLength() {
        return this.allocatedIpAddress.prefixLength;
    }

    @Override
    public String getHostId() {
        return this.hostInfo.objectId;
    }

    @Override
    public String getHostName() {
        return this.hostInfo.name;
    }

    @Override
    public String getVmId() {
        return this.vmId;
    }

    @Override
    public String getHostVsmId() {
        return this.hostInfo.vsmUuid;
    }

    @Override
    public String getStatus() {
        return this.serviceStatus.status;
    }
}