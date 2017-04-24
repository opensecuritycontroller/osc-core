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
package org.osc.core.broker.service.response;

import java.util.HashMap;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.osc.sdk.controller.FlowInfo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description="Contains a map of key/values. Key can be IP,MAC,VM-UUID or unique-request-identifier in case "
        + "of Flow based query")
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class QueryVmInfoResponse implements Response {

    @XmlRootElement
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class VmInfo {
        public String vmName;
        public String vmId;
        public String vmUuid;
        public String vmIpAddress;
        public String vmMacAddress;
        public String hostName;
        public String hostId;

        public VmInfo() {

        }

        @Override
        public String toString() {
            return "VmInfo [vmName=" + this.vmName + ", vmId=" + this.vmId + ", vmUuid=" + this.vmUuid + ", vmIpAddress=" + this.vmIpAddress
                    + ", hostName=" + this.hostName + ", hostId=" + this.hostId + ", vmMacAddress=" + this.vmMacAddress + "]";
        }

    }

    @XmlRootElement
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class FlowVmInfo {
        @ApiModelProperty(value = "A request unique identifier")
        public String requestId;

        @ApiModelProperty(value = "The flow used to query VM info")
        public FlowInfo flow;

        @ApiModelProperty(value = "The VM info corresponding to the flow source")
        public VmInfo sourceVmInfo;
        @ApiModelProperty(value = "The VM info corresponding to the flow destination")
        public VmInfo destinationVmInfo;

        @Override
        public String toString() {
            return "FlowVmInfo [requestId=" + this.requestId + ", flow=" + this.flow + ", sourceVmInfo=" + this.sourceVmInfo
                    + ", destinationVmInfo=" + this.destinationVmInfo + "]";
        }

    }

    @ApiModelProperty(value = "A map containing the query identifier key (IP, MAC, VM UUID) and the value "
            + "holding the VM info")
    public HashMap<String, VmInfo> vmInfo = new HashMap<>();
    @ApiModelProperty(value = "A map containing a flow based request unique identifier key and the value holding "
            + "holding the VM info")
    public HashMap<String, FlowVmInfo> flowVmInfo = new HashMap<>();

    @Override
    public String toString() {
        return "QueryVmInfoResponse [vmInfo=" + this.vmInfo + ", flowVmInfo=" + this.flowVmInfo + "]";
    }

}
