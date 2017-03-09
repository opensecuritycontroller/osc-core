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
package org.osc.core.broker.model.entities.appliance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapKeyColumn;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.osc.core.broker.model.entities.BaseEntity;

@Entity
@Table(name = "APPLIANCE_SOFTWARE_VERSION", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "appliance_fk", "appliance_software_version", "virtualization_type",
                "virtualization_software_version" }), @UniqueConstraint(columnNames = { "image_url" }) })
public class ApplianceSoftwareVersion extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "appliance_fk", nullable = false,
        foreignKey = @ForeignKey(name = "FK_AV_APPLIANCE"))
    // name our own index
    private Appliance appliance;

    @Column(name = "appliance_software_version", nullable = false)
    private String applianceSoftwareVersion;

    @Column(name = "virtualization_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private VirtualizationType virtualizationType;

    @Column(name = "virtualization_software_version", nullable = false)
    private String virtualizationSoftwareVersion;

    @Column(name = "image_url", nullable = false)
    private String imageUrl;

    @Column(name = "minimum_cpus")
    private Integer minCpus;

    @Column(name = "memory_in_mb")
    private Integer memoryInMb;

    @Column(name = "disk_in_gb")
    private Integer diskSizeInGb;

    /**
     * Specifies whether there is seperate nic needed for ingress/egress.
     */
    @Column(name = "additional_nic_for_inspection", nullable = false)
    private Boolean additionalNicForInspection = false;

    @ElementCollection(fetch = FetchType.EAGER)
    @Column(name = "encapsulation_type")
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "APPLIANCE_SOFTWARE_VERSION_ENCAPSULATION_TYPE_ATTR",
            joinColumns = @JoinColumn(name = "appliance_software_version_fk"),
            foreignKey = @ForeignKey(name = "FK_ASV_ASV_ENCAPSULATION"))
    private List<TagEncapsulationType> encapsulationTypes = new ArrayList<TagEncapsulationType>();

    @ElementCollection(fetch = FetchType.EAGER)
    @MapKeyColumn(name = "key")
    @Column(name = "value")
    @CollectionTable(name = "APPLIANCE_SOFTWARE_VERSION_IMAGE_PROPERTIES", joinColumns = @JoinColumn(name = "asv_fk"))
    private Map<String, String> imageProperties = new HashMap<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @MapKeyColumn(name = "key")
    @Column(name = "value")
    @CollectionTable(name = "APPLIANCE_SOFTWARE_VERSION_CONFIG_PROPERTIES", joinColumns = @JoinColumn(name = "asv_fk"))
    private Map<String, String> configProperties = new HashMap<>();

    public ApplianceSoftwareVersion(Appliance appliance) {
        super();

        this.appliance = appliance;
    }

    public ApplianceSoftwareVersion() { // default constructor is required for
                                        // Hibernate dynamic query
        super();
    }

    /**
     * @return the appliance
     */
    public Appliance getAppliance() {
        return this.appliance;
    }

    /**
     * @param appliance
     *            the appliance to set
     */
    void setAppliance(Appliance appliance) {
        this.appliance = appliance;
    }

    /**
     * @return the applianceSoftwareVersion
     */
    public String getApplianceSoftwareVersion() {
        return this.applianceSoftwareVersion;
    }

    /**
     * @param applianceSoftwareVersion
     *            the applianceSoftwareVersion to set
     */
    public void setApplianceSoftwareVersion(String applianceSoftwareVersion) {
        this.applianceSoftwareVersion = applianceSoftwareVersion;
    }

    /**
     * @return the virtualizationType
     */
    public VirtualizationType getVirtualizationType() {
        return this.virtualizationType;
    }

    /**
     * @param virtualizationType
     *            the virtualizationType to set
     */
    public void setVirtualizationType(VirtualizationType virtualizationType) {
        this.virtualizationType = virtualizationType;
    }

    /**
     * @return the virtualizarionSoftwareVersion
     */
    public String getVirtualizarionSoftwareVersion() {
        return this.virtualizationSoftwareVersion;
    }

    /**
     * @param virtualizarionSoftwareVersion
     *            the virtualizarionSoftwareVersion to set
     */
    public void setVirtualizarionSoftwareVersion(String virtualizarionSoftwareVersion) {
        this.virtualizationSoftwareVersion = virtualizarionSoftwareVersion;
    }

    /**
     * @return the image url
     */
    public String getImageUrl() {
        return this.imageUrl;
    }

    /**
     * @param imagefile
     *            the image url to set
     */
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
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

    public boolean hasAdditionalNicForInspection() {
        return this.additionalNicForInspection;
    }

    public void setAdditionalNicForInspection(boolean additionalNicForInspection) {
        this.additionalNicForInspection = additionalNicForInspection;
    }

    public List<TagEncapsulationType> getEncapsulationTypes() {
        return this.encapsulationTypes;
    }

    public void setEncapsulationTypes(List<TagEncapsulationType> encapsulationType) {
        this.encapsulationTypes = encapsulationType;
    }

    public Map<String, String> getImageProperties() {
        return this.imageProperties;
    }

    public Map<String, String> getConfigProperties() {
        return this.configProperties;
    }

    @Override
    public String toString() {
        return "ApplianceSoftwareVersion [appliance=" + this.appliance + ", applianceSoftwareVersion="
                + this.applianceSoftwareVersion + ", virtualizationType=" + this.virtualizationType
                + ", virtualizationSoftwareVersion=" + this.virtualizationSoftwareVersion + ", imageUrl=" + this.imageUrl
                + ", minCpus=" + this.minCpus + ", memoryInMb=" + this.memoryInMb + ", diskSizeInGb=" + this.diskSizeInGb
                + ", encapsulationTypes=" + this.encapsulationTypes + ", imageProperties=" + this.imageProperties
                + ", configProperties=" + this.configProperties + "getId()=" + getId() + "]";
    }

    public String getApplianceModel() {
        return this.appliance.getModel();
    }
}