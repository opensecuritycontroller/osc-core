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

import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import io.swagger.annotations.ApiModelProperty;

@XmlRootElement(name = "securityGroupMember")
@XmlAccessorType(XmlAccessType.FIELD)
public class SecurityGroupMemberItemDto extends BaseDto {

    @ApiModelProperty(required = true)
    private String name;

    @ApiModelProperty(required = false)
    private String region;

    @ApiModelProperty(required = false)
    private String openstackId;

    @ApiModelProperty(required = true)
    private String type;

    @ApiModelProperty(required = false,
            value = " Protect router port to inspect external traffic coming to this subnet")
    private boolean protectExternal;

    @ApiModelProperty(required = false,
            value = " This field only applies to SGM type 'Subnet' to represent Network UUID this subnet belongs to")
    private String parentOpenStackId;

    @ApiModelProperty(required = false)
    @XmlElement(name = "port")
    private Set<PortDto> ports = new HashSet<>();

    @ApiModelProperty(required = false,
            value = "Protects all Kubernetes pods labeled with this value.")
    private String label;

    public SecurityGroupMemberItemDto() {
    }

    public SecurityGroupMemberItemDto(String region, String name, String openstackId, String type,
            boolean protectExternal) {
        super();
        this.name = name;
        this.region = region;
        this.openstackId = openstackId;
        this.type = type;
        this.protectExternal = protectExternal;
    }

    public SecurityGroupMemberItemDto(String region, String name, String openstackId, String type,
            boolean protectExternal, String parentOpenStackId) {
        super();
        this.name = name;
        this.region = region;
        this.openstackId = openstackId;
        this.type = type;
        this.protectExternal = protectExternal;
        this.parentOpenStackId = parentOpenStackId;
    }

    public SecurityGroupMemberItemDto(String name, String type, String label) {
        super();
        this.name = name;
        this.type = type;
        this.label = label;
    }


    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRegion() {
        return this.region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getOpenstackId() {
        return this.openstackId;
    }

    public void setOpenstackId(String openstackId) {
        this.openstackId = openstackId;
    }

    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getLabel() {
        return this.label;
    }

    public boolean isProtectExternal() {
        return this.protectExternal;
    }

    public void setProtectExternal(boolean mode) {
        this.protectExternal = mode;
    }

    public String getParentOpenStackId() {
        return this.parentOpenStackId;
    }

    public void setParentOpenStackId(String parentOpenStackId) {
        this.parentOpenStackId = parentOpenStackId;
    }

    public Set<PortDto> getPorts() {
        return this.ports;
    }

    public void setPorts(Set<PortDto> ports) {
        this.ports = ports;
    }

    @Override
    public String toString() {
        return "SecurityGroupMemberItemDto [name=" + this.name + ", region=" + this.region + ", openstackId=" + this.openstackId
                + ", type=" + this.type + ", protectExternal=" + this.protectExternal + ", parentOpenStackId=" + this.parentOpenStackId
                + ", ports=" + this.ports + "]";
    }

}
