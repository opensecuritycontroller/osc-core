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
package org.osc.core.broker.service.securitygroup;

import io.swagger.annotations.ApiModelProperty;

import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.osc.core.broker.model.entities.virtualization.SecurityGroupMemberType;
import org.osc.core.broker.service.dto.BaseDto;
import org.osc.core.broker.service.exceptions.VmidcBrokerInvalidEntryException;
import org.osc.core.broker.util.ValidateUtil;

@XmlRootElement(name = "securityGroupMember")
@XmlAccessorType(XmlAccessType.FIELD)
public class SecurityGroupMemberItemDto extends BaseDto {

    @ApiModelProperty(required = true)
    private String name;

    @ApiModelProperty(required = true)
    private String region;

    @ApiModelProperty(required = true)
    private String openstackId;

    @ApiModelProperty(required = true)
    private SecurityGroupMemberType type;

    @ApiModelProperty(required = false,
            value = " Protect router port to inspect external traffic coming to this subnet")
    private boolean protectExternal;

    @ApiModelProperty(required = false,
            value = " This field only applies to SGM type 'Subnet' to represent Network UUID this subnet belongs to")
    private String parentOpenStackId;

    public SecurityGroupMemberItemDto() {
    }

    public SecurityGroupMemberItemDto(String region, String name, String openstackId, SecurityGroupMemberType type,
            boolean protectExternal) {
        super();
        this.name = name;
        this.region = region;
        this.openstackId = openstackId;
        this.type = type;
        this.protectExternal = protectExternal;
    }

    public SecurityGroupMemberItemDto(String region, String name, String openstackId, SecurityGroupMemberType type,
            boolean protectExternal, String parentOpenStackId) {
        super();
        this.name = name;
        this.region = region;
        this.openstackId = openstackId;
        this.type = type;
        this.protectExternal = protectExternal;
        this.parentOpenStackId = parentOpenStackId;
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

    public SecurityGroupMemberType getType() {
        return this.type;
    }

    public void setType(SecurityGroupMemberType type) {
        this.type = type;
    }

    public boolean isProtectExternal() {
        return protectExternal;
    }

    public void setProtectExternal(boolean mode) {
        this.protectExternal = mode;
    }

    public String getParentOpenStackId() {
        return parentOpenStackId;
    }

    public void setParentOpenStackId(String parentOpenStackId) {
        this.parentOpenStackId = parentOpenStackId;
    }

    public static void checkForNullFields(SecurityGroupMemberItemDto dto) throws VmidcBrokerInvalidEntryException {

        // build a map of (field,value) pairs to be checked for null/empty
        // values
        Map<String, Object> map = new HashMap<String, Object>();

        map.put("Name", dto.getName());
        map.put("Region", dto.getRegion());
        map.put("Openstack Id", dto.getOpenstackId());
        map.put("Type", dto.getType());

        if (dto.getType().equals(SecurityGroupMemberType.SUBNET)) {
            map.put("Network  Id", dto.getParentOpenStackId());
        }

        ValidateUtil.checkForNullFields(map);
    }

}
