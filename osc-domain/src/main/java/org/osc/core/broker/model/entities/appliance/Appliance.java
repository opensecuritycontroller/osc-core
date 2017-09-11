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

import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.osc.core.broker.model.entities.BaseEntity;

@Entity
@Table(name = "APPLIANCE")
public class Appliance extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Column(name = "model", unique = true, nullable = false)
    private String model;

    @Column(name = "manager_type", nullable = false)
    private String managerType;

    @Column(name = "manager_software_version", nullable = false)
    private String managerSoftwareVersion;

    @OneToMany(mappedBy = "appliance", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<ApplianceSoftwareVersion> applianceSoftwareVersions = new HashSet<ApplianceSoftwareVersion>();

    public Appliance() {
        super();
    }

    public Appliance(String model, String managerType, String managerSoftwareVersion) {
        super();
        this.model = model;
        this.managerType = managerType;
        this.managerSoftwareVersion = managerSoftwareVersion;
    }

    public void addApplianceSoftwareVersion(ApplianceSoftwareVersion applianceSoftwareVersion) {
        this.applianceSoftwareVersions.add(applianceSoftwareVersion);
        applianceSoftwareVersion.setAppliance(this);
    }

    public void removeApplianceSoftwareVersion(ApplianceSoftwareVersion applianceSoftwareVersion) {
        this.applianceSoftwareVersions.remove(applianceSoftwareVersion);
    }

    public String getManagerSoftwareVersion() {
        return this.managerSoftwareVersion;
    }

    public void setManagerSoftwareVersion(String managerSoftwareVersion) {
        this.managerSoftwareVersion = managerSoftwareVersion;
    }

    public String getModel() {
        return this.model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    @Override
    public String toString() {
        return "Appliance [model=" + this.model + ", managerType=" + this.managerType + ", managerSoftwareVersion="
                + this.managerSoftwareVersion + ", getId()=" + getId() + "]";
    }

    public String getManagerType() {
        return this.managerType;
    }

    public void setManagerType(String managerType) {
        this.managerType = managerType;
    }

    public Set<ApplianceSoftwareVersion> getApplianceVersions() {
        return this.applianceSoftwareVersions;
    }

}