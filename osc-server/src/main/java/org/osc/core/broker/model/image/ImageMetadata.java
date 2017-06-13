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
package org.osc.core.broker.model.image;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osc.core.broker.model.plugin.manager.ManagerType;
import org.osc.core.broker.model.virtualization.OpenstackSoftwareVersion;
import org.osc.core.broker.service.dto.VirtualizationType;
import org.osc.core.broker.util.ValidateUtil;
import org.osc.core.util.VersionUtil.Version;
import org.osc.sdk.controller.TagEncapsulationType;

public class ImageMetadata {

    public static final String META_FILE_NAME = "meta.json";

    private String metaDataVersion;

    private String model;

    private String managerType;
    private String managerVersion;

    private String virtualizationType;
    private String virtualizationVersion;

    private String softwareVersion;
    private String imageName;
    private Version minIscVersion;

    private Integer minCpus;
    private Integer memoryInMb;
    private Integer diskSizeInGb;

    private boolean additionalNicForInspection;

    private List<String> encapsulationTypes = new ArrayList<String>();

    /**
     * The key value pair properties will be included as part glance image properties.
     */
    private Map<String, String> imageProperties = new HashMap<>();

    /**
     * The config properties will be included in the config-drive content file
     */
    private Map<String, String> configProperties = new HashMap<>();

    public ImageMetadata() {

    }

    public String getMetaDataVersion() {
        return this.metaDataVersion;
    }

    public String getModel() {
        return this.model;
    }

    public String getManagerVersion() {
        return this.managerVersion;
    }

    public String getSoftwareVersion() {
        return this.softwareVersion;
    }

    public Integer getMinCpus() {
        return this.minCpus;
    }

    public Integer getMemoryInMb() {
        return this.memoryInMb;
    }

    public Integer getDiskSizeInGb() {
        return this.diskSizeInGb;
    }

    public Version getMinIscVersion() {
        return this.minIscVersion;
    }

    public String getManagerTypeString() {
        return this.managerType;
    }

    public String getVirtualizationTypeString() {
        return this.virtualizationType;
    }

    public String getVirtualizationVersionString() {
        return this.virtualizationVersion;
    }

    public ManagerType getManagerType() {
        return ManagerType.fromText(this.managerType);
    }

    public VirtualizationType getVirtualizationType() {
        return VirtualizationType.fromText(this.virtualizationType);
    }

    public OpenstackSoftwareVersion getOpenstackVirtualizationVersion() {
        return OpenstackSoftwareVersion.fromText(this.virtualizationVersion);
    }

    public List<TagEncapsulationType> getEncapsulationTypes() {
        List<TagEncapsulationType> typesEnum = new ArrayList<TagEncapsulationType>();
        for (String type : this.encapsulationTypes) {
            typesEnum.add(TagEncapsulationType.fromText(type));
        }

        return typesEnum;
    }

    public void setMetaDataVersion(String metaDataVersion) {
        this.metaDataVersion = metaDataVersion;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public void setManagerType(String managerType) {
        this.managerType = managerType;
    }

    public void setManagerVersion(String managerVersion) {
        this.managerVersion = managerVersion;
    }

    public void setVirtualizationType(String virtualizationType) {
        this.virtualizationType = virtualizationType;
    }

    public void setVirtualizationVersion(String virtualizationVersion) {
        this.virtualizationVersion = virtualizationVersion;
    }

    public void setSoftwareVersion(String softwareVersion) {
        this.softwareVersion = softwareVersion;
    }

    public String getImageName() {
        return this.imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    public void setMinIscVersion(Version minIscVersion) {
        this.minIscVersion = minIscVersion;
    }

    public void setMinCpus(int minCpus) {
        this.minCpus = minCpus;
    }

    public void setMemoryInMb(int memoryInMb) {
        this.memoryInMb = memoryInMb;
    }

    public void setDiskSizeInGb(int diskSizeInGb) {
        this.diskSizeInGb = diskSizeInGb;
    }

    public Map<String, String> getImageProperties() {
        return this.imageProperties;
    }

    public void setImageProperties(Map<String, String> glanceProperties) {
        this.imageProperties = glanceProperties;
    }

    public Map<String, String> getConfigProperties() {
        return this.configProperties;
    }

    public void setConfigProperties(Map<String, String> configProperties) {
        this.configProperties = configProperties;
    }

    public boolean hasAdditionalNicForInspection() {
        return this.additionalNicForInspection;
    }

    public static void checkForNullFields(ImageMetadata dto) throws Exception {
        Map<String, Object> notNullFieldsMap = new HashMap<String, Object>();
        notNullFieldsMap.put("Image Name", dto.getImageName());
        notNullFieldsMap.put("Software Version", dto.getSoftwareVersion());
        notNullFieldsMap.put("Manager Version", dto.getManagerVersion());
        notNullFieldsMap.put("Model", dto.getModel());
        notNullFieldsMap.put("Metadata Version", dto.getMetaDataVersion());
        notNullFieldsMap.put("Virtualization Type", dto.getVirtualizationTypeString());
        notNullFieldsMap.put("Virtualization Version", dto.getVirtualizationVersionString());
        notNullFieldsMap.put("Manager Type", dto.getManagerTypeString());
        notNullFieldsMap.put("Minimum OSC Version", dto.getMinIscVersion());
        notNullFieldsMap.put("Disk Size", dto.getDiskSizeInGb());
        notNullFieldsMap.put("Memory", dto.getMemoryInMb());
        notNullFieldsMap.put("Minimum CPU", dto.getMinCpus());

        ValidateUtil.checkForNullFields(notNullFieldsMap);
    }

}
