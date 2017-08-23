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
package org.osc.core.broker.service.dto;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import io.swagger.annotations.ApiModelProperty;

@XmlRootElement(name = "port")
@XmlAccessorType(XmlAccessType.FIELD)
public class PortDto extends BaseDto {

    @ApiModelProperty(value = "Mac address assigned to this port.")
    private String macAddress;

    private String openstackId;

    @ApiModelProperty(value = "List or object ip addresses assigned to this port's mac address ")
    private Set<String> ipAddresses = new HashSet<>();

    public PortDto(Long id, String openStackId, String macAddress, Collection<String> ipAddresses) {
        super(id);
        this.openstackId = openStackId;
        this.macAddress = macAddress;
        this.ipAddresses = new HashSet<>(ipAddresses);
    }

    public Set<String> getIpAddresses() {
        return this.ipAddresses;
    }

    public String getMacAddress() {
        return this.macAddress;
    }

    public String getOpenstackId() {
        return this.openstackId;
    }

    @Override
    public String toString() {
        return "VMPortDto [id=" + getId() + ", macAddress=" + this.macAddress + ", ipAddresses=" + this.ipAddresses + "]";
    }

}
