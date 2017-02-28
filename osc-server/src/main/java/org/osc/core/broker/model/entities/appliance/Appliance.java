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
import org.osc.core.broker.model.plugin.manager.ManagerType;
import org.osc.sdk.manager.element.ApplianceElement;

@Entity
@Table(name = "APPLIANCE")
public class Appliance extends BaseEntity implements ApplianceElement {

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

    public void addApplianceSoftwareVersion(ApplianceSoftwareVersion applianceSoftwareVersion) {
        applianceSoftwareVersions.add(applianceSoftwareVersion);
        applianceSoftwareVersion.setAppliance(this);
    }

    public void removeApplianceSoftwareVersion(ApplianceSoftwareVersion applianceSoftwareVersion) {
        applianceSoftwareVersions.remove(applianceSoftwareVersion);
    }

    public String getManagerSoftwareVersion() {
        return managerSoftwareVersion;
    }

    public void setManagerSoftwareVersion(String managerSoftwareVersion) {
        this.managerSoftwareVersion = managerSoftwareVersion;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    @Override
    public String toString() {
        return "Appliance [model=" + model + ", managerType=" + managerType + ", managerSoftwareVersion="
                + managerSoftwareVersion + ", getId()=" + getId() + "]";
    }

    public ManagerType getManagerType() {
        return ManagerType.fromText(managerType);
    }

    public void setManagerType(ManagerType managerType) {
        this.managerType = managerType.getValue();
    }

    public Set<ApplianceSoftwareVersion> getApplianceVersions() {
        return applianceSoftwareVersions;
    }

}
