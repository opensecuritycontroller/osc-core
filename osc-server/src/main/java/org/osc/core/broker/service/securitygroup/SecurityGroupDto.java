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
package org.osc.core.broker.service.securitygroup;

import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.osc.core.broker.job.JobState;
import org.osc.core.broker.job.JobStatus;
import org.osc.core.broker.service.dto.BaseDto;
import org.osc.core.broker.util.ValidateUtil;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description="Parent Id is applicable for this object. The corresponding virtualization connector is considered"
        + " the parent of this Security Group.")
@XmlRootElement(name = "securityGroup")
@XmlAccessorType(XmlAccessType.FIELD)
public class SecurityGroupDto extends BaseDto {

    @ApiModelProperty(required = true)
    private String name;

    @ApiModelProperty(required = true)
    private String tenantId;

    @ApiModelProperty(required = true)
    private String tenantName;

    @ApiModelProperty(readOnly = true)
    private boolean markForDeletion = false;

    private boolean protectAll;

    @ApiModelProperty(readOnly = true)
    private String servicesDescription;

    @ApiModelProperty(readOnly = true)
    private String memberDescription;

    private String virtualizationConnectorName;

    @ApiModelProperty(readOnly = true)
    private JobState lastJobState;

    @ApiModelProperty(readOnly = true)
    private JobStatus lastJobStatus;

    @ApiModelProperty(readOnly = true)
    private Long lastJobId;

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTenantId() {
        return this.tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getTenantName() {
        return this.tenantName;
    }

    public void setTenantName(String tenantName) {
        this.tenantName = tenantName;
    }

    public boolean isMarkForDeletion() {
        return this.markForDeletion;
    }

    public void setMarkForDeletion(boolean markForDeletion) {
        this.markForDeletion = markForDeletion;
    }

    public boolean isProtectAll() {
        return this.protectAll;
    }

    public void setProtectAll(boolean protectAll) {
        this.protectAll = protectAll;
    }

    public String getServicesDescription() {
        return this.servicesDescription;
    }

    public void setServicesDescription(String servicesDescription) {
        this.servicesDescription = servicesDescription;
    }

    public String getMemberDescription() {
        return this.memberDescription;
    }

    public void setMemberDescription(String memberDescription) {
        this.memberDescription = memberDescription;
    }

    public String getVirtualizationConnectorName() {
        return this.virtualizationConnectorName;
    }

    public void setVirtualizationConnectorName(String virtualizationConnectorName) {
        this.virtualizationConnectorName = virtualizationConnectorName;
    }

    public JobState getLastJobState() {
        return this.lastJobState;
    }

    public void setLastJobState(JobState lastJobState) {
        this.lastJobState = lastJobState;
    }

    public JobStatus getLastJobStatus() {
        return this.lastJobStatus;
    }

    public void setLastJobStatus(JobStatus lastJobStatus) {
        this.lastJobStatus = lastJobStatus;
    }

    public Long getLastJobId() {
        return this.lastJobId;
    }

    public void setLastJobId(Long lastJobId) {
        this.lastJobId = lastJobId;
    }

    public static void checkForNullIdFields(SecurityGroupDto dto) throws Exception {

        // build a map of (field,value) pairs to be checked for null/empty
        // values
        Map<String, Object> map = new HashMap<String, Object>();

        map.put("Id", dto.getId());
        map.put("Virtualization Connector Id", dto.getParentId());

        ValidateUtil.checkForNullFields(map);
    }

    public static void checkForNullFields(SecurityGroupDto dto) throws Exception {

        // build a map of (field,value) pairs to be checked for null/empty
        // values
        Map<String, Object> map = new HashMap<String, Object>();

        map.put("Name", dto.getName());
        map.put("Tenant Id", dto.getTenantId());
        map.put("Tenant Name", dto.getTenantName());

        ValidateUtil.checkForNullFields(map);
    }

    public static void checkFieldLength(SecurityGroupDto dto) throws Exception {

        Map<String, String> map = new HashMap<String, String>();

        map.put("Name", dto.getName());

        ValidateUtil.validateFieldLength(map, ValidateUtil.DEFAULT_MAX_LEN);
    }
}
