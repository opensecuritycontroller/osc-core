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
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.osc.core.broker.model.entities.BaseEntity;
import org.osc.core.broker.model.entities.job.JobRecord;
import org.osc.core.broker.model.entities.job.LastJobContainer;
import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;

@Entity
@Table(name = "DISTRIBUTED_APPLIANCE")
public class DistributedAppliance extends BaseEntity implements LastJobContainer {

    private static final long serialVersionUID = 1L;

    @Column(name = "name", unique = true, nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "appliance_manager_connector_fk", nullable = false,
            foreignKey = @ForeignKey(name = "FK_DA_APPLIANCE_MANAGER_CONNECTOR"))

    // name our own index
    private ApplianceManagerConnector applianceManagerConnector;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "appliance_fk", nullable = false,
            foreignKey = @ForeignKey(name = "FK_DA_APPLIANCE"))

    // name our own index
    private Appliance appliance;

    @Column(name = "appliance_version", nullable = false)
    private String applianceVersion;

    @OneToMany(mappedBy = "distributedAppliance", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<VirtualSystem> virtualSystems = new HashSet<VirtualSystem>();

    @Column(name = "mgr_secret_key")
    private String mgrSecretKey;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_job_id_fk", foreignKey = @ForeignKey(name = "FK_DA_LAST_JOB"))
    private JobRecord lastJob;

    public DistributedAppliance() {
        super();
    }

    public DistributedAppliance(ApplianceManagerConnector applianceManagerConnector) {
        super();
        this.applianceManagerConnector = applianceManagerConnector;
    }

    public Appliance getAppliance() {
        return this.appliance;
    }

    public String getApplianceVersion() {
        return this.applianceVersion;
    }

    public void setApplianceVersion(String applianceVersion) {
        this.applianceVersion = applianceVersion;
    }

    public void setAppliance(Appliance appliance) {
        this.appliance = appliance;
    }

    public void addVirtualSystem(VirtualSystem virtualSystem) {
        this.virtualSystems.add(virtualSystem);
        virtualSystem.setDistributedAppliance(this);
    }

    public void removeVirtualSystem(VirtualSystem virtualSystem) {
        this.virtualSystems.remove(virtualSystem);
    }

    public Set<VirtualSystem> getVirtualSystems() {
        return this.virtualSystems;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ApplianceManagerConnector getApplianceManagerConnector() {
        return this.applianceManagerConnector;
    }

    public void setApplianceManagerConnector(ApplianceManagerConnector applianceManagerConnector) {
        this.applianceManagerConnector = applianceManagerConnector;
    }

    public String getMgrSecretKey() {
        return this.mgrSecretKey;
    }

    public void setMgrSecretKey(String mgrSecretKey) {
        this.mgrSecretKey = mgrSecretKey;
    }

    @Override
    public JobRecord getLastJob() {
        return this.lastJob;
    }

    @Override
    public void setLastJob(JobRecord lastJob) {
        this.lastJob = lastJob;
    }

    @Override
    public String toString() {
        return "DistributedAppliance [name=" + this.name + ", applianceManagerConnector="
                + this.applianceManagerConnector + ", appliance=" + this.appliance + ", applianceVersion="
                + this.applianceVersion + ", mgrSecretKey=" + this.mgrSecretKey + "]";
    }

}