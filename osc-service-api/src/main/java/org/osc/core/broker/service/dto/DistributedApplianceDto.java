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

import org.osc.core.broker.service.annotations.VmidcLogHidden;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description="Parent Id is not applicable for this object")
@XmlRootElement(name = "distributedAppliance")
@XmlAccessorType(XmlAccessType.FIELD)
public class DistributedApplianceDto extends BaseDto {

    @ApiModelProperty(required=true)
    private String name;

    @ApiModelProperty(required=true)
    private Long managerConnectorId;
    @ApiModelProperty(readOnly=true)
    private String managerConnectorName = "";

    @ApiModelProperty(required=true)
    private Long applianceSoftwareVersionId;
    @ApiModelProperty(readOnly = true)
    private String applianceSoftwareVersionName = "";
    @ApiModelProperty(readOnly=true)
    private String applianceModel = "";

    @ApiModelProperty(hidden=true)
    @VmidcLogHidden
    private String secretKey = "";

    @ApiModelProperty(value="At list one or more element must be provided", required=true)
    @XmlElement(name = "virtualSystem")
    private Set<VirtualSystemDto> virtualizationSystems = new HashSet<VirtualSystemDto>();

    @ApiModelProperty(readOnly=true)
    private String lastJobState;
    @ApiModelProperty(readOnly=true)
    private String lastJobStatus;
    @ApiModelProperty(readOnly=true)
    private Long lastJobId;

    @ApiModelProperty(readOnly=true)
    private boolean markForDeletion = false;

    public DistributedApplianceDto(Long mcId) {
        super();
        this.managerConnectorId = mcId;
    }

    public DistributedApplianceDto() {
        super();
    }

    public Long getApplianceId() {
        return this.applianceSoftwareVersionId;
    }

    public void setApplianceId(Long applianceId) {
        this.applianceSoftwareVersionId = applianceId;
    }

    public Long getMcId() {
        return this.managerConnectorId;
    }

    public void setMcId(Long mcId) {
        this.managerConnectorId = mcId;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getApplianceManagerConnectorName() {
        return this.managerConnectorName;
    }

    public void setApplianceManagerConnectorName(String applianceManagerConnectorName) {
        this.managerConnectorName = applianceManagerConnectorName;
    }

    public String getApplianceModel() {
        return this.applianceModel;
    }

    public void setApplianceModel(String applianceModel) {
        this.applianceModel = applianceModel;
    }

    public String getApplianceSoftwareVersionName() {
        return this.applianceSoftwareVersionName;
    }

    public void setApplianceSoftwareVersionName(String applianceSoftwareVersionName) {
        this.applianceSoftwareVersionName = applianceSoftwareVersionName;
    }

    public String getSecretKey() {
        return this.secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public Set<VirtualSystemDto> getVirtualizationSystems() {
        return this.virtualizationSystems;
    }

    public void setVirtualizationSystems(Set<VirtualSystemDto> virtualizationSystems) {
        this.virtualizationSystems = virtualizationSystems;
    }

    public boolean isMarkForDeletion() {
        return this.markForDeletion;
    }

    public void setMarkForDeletion(boolean markForDeletion) {
        this.markForDeletion = markForDeletion;
    }

    public String getLastJobStatus() {
        return this.lastJobStatus;
    }

    public void setLastJobStatus(String lastJobStatus) {
        this.lastJobStatus = lastJobStatus;
    }

    public String getLastJobState() {
        return this.lastJobState;
    }

    public void setLastJobState(String lastJobState) {
        this.lastJobState = lastJobState;
    }

    public Long getLastJobId() {
        return this.lastJobId;
    }

    public void setLastJobId(Long lastJobId) {
        this.lastJobId = lastJobId;
    }

    @Override
    public String toString() {
        return "DistributedApplianceDto [id=" + getId() + ", name=" + this.name + ", applianceManagerConnectorName="
                + this.managerConnectorName + ", mcId=" + this.managerConnectorId + ", applianceId=" + this.applianceSoftwareVersionId
                + ", applianceModel=" + this.applianceModel + ", applianceSoftwareVersionName=" + this.applianceSoftwareVersionName + ", secretKey="
                + this.secretKey + ", lastJobstatus=" + this.lastJobStatus + ", markForDeletion="
                + this.markForDeletion + "]";
    }

    public static void sanitizeDistributedAppliance(DistributedApplianceDto dto) {
        dto.setSecretKey(null);
    }

}
