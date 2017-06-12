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
package org.osc.core.broker.model.entities.virtualization.openstack;

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
import javax.persistence.UniqueConstraint;

import org.osc.core.broker.model.entities.BaseEntity;
import org.osc.core.broker.model.entities.appliance.DistributedApplianceInstance;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.job.JobRecord;
import org.osc.core.broker.model.entities.job.LastJobContainer;

@Entity
@Table(name = "DEPLOYMENT_SPEC", uniqueConstraints = { @UniqueConstraint(columnNames = { "vs_fk", "tenant_id",
        "region" }) })
public class DeploymentSpec extends BaseEntity implements LastJobContainer {

    private static final long serialVersionUID = 1L;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "region", nullable = false)
    private String region;

    @Column(name = "tenant_name", nullable = false)
    private String tenantName;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "management_network_name", nullable = false)
    private String managementNetworkName;

    @Column(name = "management_network_id", nullable = false)
    private String managementNetworkId;

    @Column(name = "inspection_network_name", nullable = false)
    private String inspectionNetworkName;

    @Column(name = "inspection_network_id", nullable = false)
    private String inspectionNetworkId;

    @Column(name = "floating_pool_name")
    private String floatingIpPoolName;

    @Column(name = "instance_count", nullable = false)
    private int instanceCount = 1;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "last_job_id_fk", foreignKey = @ForeignKey(name = "FK_DS_LAST_JOB"))
    private JobRecord lastJob;

    @Column(name = "shared", columnDefinition = "bit default 0")
    private boolean isShared;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "vs_fk", nullable = false,
            foreignKey = @ForeignKey(name = "FK_DEPLOYMENT_SPEC_VS"))
    private VirtualSystem virtualSystem;

    @OneToMany(mappedBy = "deploymentSpec", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<AvailabilityZone> availabilityZones = new HashSet<AvailabilityZone>();

    @OneToMany(mappedBy = "deploymentSpec", fetch = FetchType.LAZY)
    private Set<DistributedApplianceInstance> distributedApplianceInstances = new HashSet<DistributedApplianceInstance>();

    @OneToMany(mappedBy = "deploymentSpec", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<Host> hosts = new HashSet<Host>();

    @OneToMany(mappedBy = "deploymentSpec", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<HostAggregate> hostAggregates = new HashSet<HostAggregate>();

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "os_sg_reference_fk", nullable = true,
            foreignKey = @ForeignKey(name = "FK_DS_OS_SG_REFERENCE"))
    private OsSecurityGroupReference osSecurityGroupReference;

    public DeploymentSpec(VirtualSystem virtualSystem, String region, String tenantId, String managementNetworkId,
            String inspectionNetworkId, String floatingIpPoolName) {
        this.virtualSystem = virtualSystem;
        this.region = region;
        this.tenantId = tenantId;
        this.managementNetworkId = managementNetworkId;
        this.inspectionNetworkId = inspectionNetworkId;
        this.floatingIpPoolName = floatingIpPoolName;
    }

    public DeploymentSpec() {

    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTenantId() {
        return this.tenantId;
    }

    void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getManagementNetworkId() {
        return this.managementNetworkId;
    }

    void setManagementNetworkId(String managementNetworkId) {
        this.managementNetworkId = managementNetworkId;
    }

    public VirtualSystem getVirtualSystem() {
        return this.virtualSystem;
    }

    void setVirtualSystem(VirtualSystem virtualSystem) {
        this.virtualSystem = virtualSystem;
    }

    public Set<AvailabilityZone> getAvailabilityZones() {
        return this.availabilityZones;
    }

    public void setAvailabilityZones(Set<AvailabilityZone> availabilityZones) {
        this.availabilityZones = availabilityZones;
    }

    public Set<Host> getHosts() {
        return this.hosts;
    }

    public void setHosts(Set<Host> hosts) {
        this.hosts = hosts;
    }

    public String getTenantName() {
        return this.tenantName;
    }

    public void setTenantName(String tenantName) {
        this.tenantName = tenantName;
    }

    public String getManagementNetworkName() {
        return this.managementNetworkName;
    }

    public void setManagementNetworkName(String managementNetworkName) {
        this.managementNetworkName = managementNetworkName;
    }

    public String getRegion() {
        return this.region;
    }

    void setRegion(String region) {
        this.region = region;
    }

    public Set<DistributedApplianceInstance> getDistributedApplianceInstances() {
        return this.distributedApplianceInstances;
    }

    public void setDistributedApplianceInstances(Set<DistributedApplianceInstance> dais) {
        this.distributedApplianceInstances = dais;
    }

    public int getInstanceCount() {
        return this.instanceCount;
    }

    public void setInstanceCount(int instanceCount) {
        this.instanceCount = instanceCount;
    }

    public Set<HostAggregate> getHostAggregates() {
        return this.hostAggregates;
    }

    public void setHostAggregates(Set<HostAggregate> hostAggr) {
        this.hostAggregates = hostAggr;
    }

    public OsSecurityGroupReference getOsSecurityGroupReference() {
        return this.osSecurityGroupReference;
    }

    public void setOsSecurityGroupReference(OsSecurityGroupReference osSecurityGroupReference) {
        this.osSecurityGroupReference = osSecurityGroupReference;
    }

    public boolean isShared() {
        return this.isShared;
    }

    public void setShared(boolean isShared) {
        this.isShared = isShared;
    }

    public String getInspectionNetworkName() {
        return this.inspectionNetworkName;
    }

    public void setInspectionNetworkName(String inspectionNetworkName) {
        this.inspectionNetworkName = inspectionNetworkName;
    }

    public String getInspectionNetworkId() {
        return this.inspectionNetworkId;
    }

    public void setInspectionNetworkId(String inspectionNetworkId) {
        this.inspectionNetworkId = inspectionNetworkId;
    }

    public String getFloatingIpPoolName() {
        return this.floatingIpPoolName;
    }

    public void setFloatingIpPoolName(String floatingIpPoolName) {
        this.floatingIpPoolName = floatingIpPoolName;
    }

    @Override
    public JobRecord getLastJob() {
        return this.lastJob;
    }

    @Override
    public void setLastJob(JobRecord lastJob) {
        this.lastJob = lastJob;
    }
}
