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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.osc.core.common.virtualization.VirtualizationType;
import org.osc.sdk.controller.TagEncapsulationType;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = "Parent Id is applicable for this object. "
        + "The corresponding Appliance id is considered the parent for the Appliance Software Version")
@XmlRootElement(name = "applianceSoftwareVersion")
@XmlAccessorType(XmlAccessType.FIELD)
// Appliance Software Version Data Transfer Object associated with
// ApplianceSoftwareVersion Entity
public class ApplianceSoftwareVersionDto extends BaseDto {

    @ApiModelProperty(required=true)
    private String swVersion = "";

    @ApiModelProperty(required=true)
    private VirtualizationType virtualizationType;

    @ApiModelProperty(required=true)
    private String virtualizationVersion = "";

    @ApiModelProperty(required=true)
    private String imageUrl = "";

    @ApiModelProperty(value="Required for Openstack image. Defines the supported list of Encapsulation Types the "
            + "Service Function Image capable supporting.<br>"
            + "The Encapsulation Type used for Packet Metadata (Policy Tag and Packet Direction).<br>"
            + "For example, for 'VLAN' Encapsulation Type, VLAN PCP field is used to signify the packet "
            + "direction (1 - packet is outgoing from inspected port to virtual switch, "
            + " 2 - packet is incoming to inspected port from the virtual switch) and the VLAN ID (VID) value is used "
            + "to signify Policy Tag the Service Function should apply when processing the packet.")
    private List<TagEncapsulationType> encapsulationTypes = new ArrayList<TagEncapsulationType>();

    @ApiModelProperty(value = "The key value pair properties will be included as part of glance image properties.")
    private Map<String, String> imageProperties = new HashMap<>();

    @ApiModelProperty(value = "The config properties will be included in the config-drive content file. "
            + "The keys are expected to be unique and cannont conflict with other properties which might be "
            + "specified as part of the basic configuration like applianceName etc.")
    private Map<String, String> configProperties = new HashMap<>();

    @ApiModelProperty(value = "The minimum amount of CPU cores needed for this image.")
    private Integer minCpus;

    @ApiModelProperty(value = "The minimum amount of memory needed for this image.")
    private Integer memoryInMb;

    @ApiModelProperty(value = "The minimum disk size(GB) needed for this image.")
    private Integer diskSizeInGb;

    @ApiModelProperty(value = "Whether we need a additional nic for inspection. In this case, the first nic is assumed"
            + " to handle ingress traffic and the second nic is assumed to handle egress traffic.")
    private boolean additionalNicForInspection;

    public ApplianceSoftwareVersionDto(Long applianceId) {
        super();
        setParentId(applianceId);
    }

    public ApplianceSoftwareVersionDto() {
        super();
    }

    public String getSwVersion() {
        return this.swVersion;
    }

    public void setSwVersion(String swVersion) {
        this.swVersion = swVersion;
    }

    public VirtualizationType getVirtualizationType() {
        return this.virtualizationType;
    }

    public void setVirtualizationType(VirtualizationType virtualizationType) {
        this.virtualizationType = virtualizationType;
    }

    public String getVirtualizationVersion() {
        return this.virtualizationVersion;
    }

    public void setVirtualizationVersion(String virtualizationVersion) {
        this.virtualizationVersion = virtualizationVersion;
    }

    public String getImageUrl() {
        return this.imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public List<TagEncapsulationType> getEncapsulationTypes() {
        return this.encapsulationTypes;
    }

    public void setEncapsulationTypes(List<TagEncapsulationType> encapsulationType) {
        this.encapsulationTypes = encapsulationType;
    }

    public int getMinCpus() {
        return this.minCpus;
    }

    public void setMinCpus(int minCpus) {
        this.minCpus = minCpus;
    }

    public int getMemoryInMb() {
        return this.memoryInMb;
    }

    public void setMemoryInMb(int memoryInMb) {
        this.memoryInMb = memoryInMb;
    }

    public int getDiskSizeInGb() {
        return this.diskSizeInGb;
    }

    public void setDiskSizeInGb(int diskSizeInGb) {
        this.diskSizeInGb = diskSizeInGb;
    }

    public Map<String, String> getImageProperties() {
        return this.imageProperties;
    }

    public Map<String, String> getConfigProperties() {
        return this.configProperties;
    }

    public boolean isAdditionalNicForInspection() {
        return this.additionalNicForInspection;
    }

    public void setAdditionalNicForInspection(boolean additionalNicForInspection) {
        this.additionalNicForInspection = additionalNicForInspection;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "ApplianceSoftwareVersionDto [id=" + getId() + ", applianceId=" + getParentId() + ", swVersion="
                + this.swVersion + ", virtualizationType=" + this.virtualizationType + ", virtualizationVersion="
                + this.virtualizationVersion + ", imageUrl=" + this.imageUrl + "]";
    }
}
