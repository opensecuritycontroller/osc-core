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
import java.util.HashMap;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.osc.sdk.controller.FlowInfo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = "Contains a list of IP and/or MAC and/or VM-UUID and/or a map of unique-request-identifier key "
        + "and flow value")
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class QueryVmInfoRequest implements Request {

    public String applianceInstanceName;

    @ApiModelProperty(value = "List of IP Addresses for which VM information is queried")
    @XmlElement(name = "ipAddress")
    public List<String> ipAddress = new ArrayList<String>();

    @ApiModelProperty(value = "List of MAC Addresses for which VM information is queried")
    @XmlElement(name = "macAddress")
    public List<String> macAddress = new ArrayList<String>();

    @ApiModelProperty(value = "List of VM UUID for which VM information is queried")
    @XmlElement(name = "vmUuid")
    public List<String> vmUuid = new ArrayList<String>();

    @ApiModelProperty(
            value = "A map of string key and FlowInfo (5-tuple + timestamp) value for which VM information is queried. "
                    + "Key value must be a unique. FlowInfo structure comrise of 6 elements:<br>"
                    + "<p>sourceIpAddress, sourcePort, destinationIpAddress, destinationPort, protocolId and flowTimestamp</p>"
            )
    @XmlElement(name = "flow")
    public HashMap<String, FlowInfo> flow = new HashMap<>();

    @Override
    public String toString() {
        return "QueryVmInfoRequest [applianceInstanceName=" + applianceInstanceName + ", ipAddress=" + ipAddress
                + ", macAddress=" + macAddress + ", vmUuid=" + vmUuid + ", flow=" + flow + "]";
    }

}
