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
package org.osc.core.broker.model.entities.virtualization;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.ForeignKey;
import org.osc.core.broker.model.entities.BaseEntity;
import org.osc.core.broker.model.entities.job.JobRecord;
import org.osc.core.broker.model.entities.job.LastJobContainer;

@SuppressWarnings("serial")
@Entity
@Table(name = "SECURITY_GROUP", uniqueConstraints = { @UniqueConstraint(columnNames = { "name", "tenant_id" }) })
public class SecurityGroup extends BaseEntity implements LastJobContainer{

    @Column(name = "name", nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "vc_fk", nullable = false)
    @ForeignKey(name = "FK_SG_VC")
    private VirtualizationConnector virtualizationConnector;

    @Column(name = "tenant_id")
    private String tenantId;

    @Column(name = "tenant_name")
    private String tenantName;

    @Column(name = "nsx_id")
    private String nsxId;

    @Column(name = "protect_all", nullable = false, columnDefinition = "bit default 1")
    private boolean protectAll = true;

    @Column(name = "mgr_id")
    private String mgrId;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "last_job_id_fk")
    @ForeignKey(name = "FK_SG_LAST_JOB")
    private JobRecord lastJob;

    /**
     * networkElementId Partner integration value like PortGroupId or Neutron SFC related value
     */
    @Column(name = "network_elem_id")
    private String networkElementId;

    @OneToMany(mappedBy = "securityGroup", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @OrderBy("type")
    private Set<SecurityGroupMember> securityGroupMembers = new TreeSet<>();

    @ManyToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinTable(name = "GROUP_INTERFACE", joinColumns = @JoinColumn(name = "security_group_fk",
            referencedColumnName = "id"), inverseJoinColumns = @JoinColumn(name = "security_group_interface_fk",
            referencedColumnName = "id"))
    private Set<SecurityGroupInterface> securityGroupInterfaces = new HashSet<SecurityGroupInterface>();

    public SecurityGroup(VirtualizationConnector virtualizationConnector, String tenantId, String tenantName) {
        this.virtualizationConnector = virtualizationConnector;
        this.tenantId = tenantId;
        this.tenantName = tenantName;
    }

    public SecurityGroup(VirtualizationConnector virtualizationConnector, String nsxId) {
        this.virtualizationConnector = virtualizationConnector;
        this.nsxId = nsxId;
    }

    SecurityGroup() {
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public VirtualizationConnector getVirtualizationConnector() {
        return this.virtualizationConnector;
    }

    void setVirtualizationConnector(VirtualizationConnector virtualizationConnector) {
        this.virtualizationConnector = virtualizationConnector;
    }

    public String getTenantId() {
        return this.tenantId;
    }

    void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getTenantName() {
        return this.tenantName;
    }

    public void setTenantName(String tenantName) {
        this.tenantName = tenantName;
    }

    public Set<SecurityGroupMember> getSecurityGroupMembers() {
        return this.securityGroupMembers;
    }

    void setSecurityGroupMembers(Set<SecurityGroupMember> securityGroupMembers) {
        this.securityGroupMembers = securityGroupMembers;
    }

    public void addSecurityGroupMember(SecurityGroupMember securityGroupMember) {
        this.securityGroupMembers.add(securityGroupMember);
    }

    public void removeSecurityGroupMember(SecurityGroupMember securityGroupMember) {
        this.securityGroupMembers.remove(securityGroupMember);
    }

    void setSecurityGroupNetworkInterfaces(Set<SecurityGroupInterface> securityGroupInterfaces) {
        this.securityGroupInterfaces = securityGroupInterfaces;
    }

    public Set<SecurityGroupInterface> getSecurityGroupInterfaces() {
        return this.securityGroupInterfaces;
    }

    public void addSecurityGroupInterface(SecurityGroupInterface securityGroupInterface) {
        this.securityGroupInterfaces.add(securityGroupInterface);
    }

    public void removeSecurityInterface(SecurityGroupInterface securityGroupInterface) {
        this.securityGroupInterfaces.remove(securityGroupInterface);
    }

    public boolean isProtectAll() {
        return this.protectAll;
    }

    public void setProtectAll(boolean protectAll) {
        this.protectAll = protectAll;
    }

    public String getNsxId() {
        return this.nsxId;
    }

    public String getMgrId() {
        return this.mgrId;
    }

    public void setMgrId(String mgrId) {
        this.mgrId = mgrId;
    }

    @Override
    public JobRecord getLastJob() {
        return this.lastJob;
    }

    @Override
    public void setLastJob(JobRecord lastJob) {
        this.lastJob = lastJob;
    }

    public String getNetworkElementId() {
        return this.networkElementId;
    }

    public void setNetworkElementId(String networkElemId) {
        this.networkElementId = networkElemId;
    }

    @Override
    public String toString() {
        return "SecurityGroup [name=" + this.name + ", virtualizationConnector=" + this.virtualizationConnector + ", tenantId="
                + this.tenantId + ", tenantName=" + this.tenantName + ", nsxId=" + this.nsxId + ", protectAll=" + this.protectAll
                + ", mgrId=" + this.mgrId + ", lastJob=" + this.lastJob + ", securityGroupMembers=" + this.securityGroupMembers
                + ", securityGroupInterfaces=" + this.securityGroupInterfaces + ", networkElementId="
                        + this.networkElementId + "]";
    }
}