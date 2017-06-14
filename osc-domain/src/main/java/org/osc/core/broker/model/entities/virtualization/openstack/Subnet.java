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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.osc.core.broker.model.entities.BaseEntity;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMember;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupMemberType;

@SuppressWarnings("serial")
@Entity
@Table(name = "OS_SUBNET")
public class Subnet extends BaseEntity implements OsProtectionEntity {

    @Column(name = "region", nullable = false)
    private String region;

    @Column(name = "network_id", nullable = false)
    private String networkId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "subnet_id", nullable = false, unique = true)
    private String openstackId;

    @Column(name = "protect_external")
    private boolean protectExternal = false;

    @OneToMany(mappedBy = "subnet", fetch = FetchType.LAZY)
    private Set<VMPort> ports = new HashSet<VMPort>();

    @OneToMany(mappedBy = "subnet", fetch = FetchType.LAZY)
    private final Set<SecurityGroupMember> securityGroupMembers = new HashSet<>();

    public Subnet(String network, String subnetId, String name, String region, boolean protectExternal) {
        super();
        this.networkId = network;
        this.region = region;
        this.openstackId = subnetId;
        this.name = name;
        this.protectExternal = protectExternal;
    }

    Subnet() {
    }

    @Override
    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getOpenstackId() {
        return this.openstackId;
    }

    public Set<VMPort> getPorts() {
        return this.ports;
    }

    void setPorts(Set<VMPort> ports) {
        this.ports = ports;
    }

    public void addPort(VMPort vmPort) {
        this.ports.add(vmPort);
    }

    public void removePort(VMPort vmPort) {
        this.ports.remove(vmPort);
    }

    @Override
    public String getRegion() {
        return this.region;
    }

    public String getNetworkId() {
        return networkId;
    }

    public void setNetworkId(String network) {
        this.networkId = network;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public void setOpenstackId(String openstackId) {
        this.openstackId = openstackId;
    }

    public boolean isProtectExternal() {
        return protectExternal;
    }

    public void setProtectExternal(boolean protectExternal) {
        this.protectExternal = protectExternal;
    }

    @Override
    public Set<SecurityGroupMember> getSecurityGroupMembers() {
        return this.securityGroupMembers;
    }

    public void addSecurityGroupMember(SecurityGroupMember member) {
        this.securityGroupMembers.add(member);
    }

    @Override
    public String toString() {
        return "Subnet [region=" + region + ", network=" + networkId + ", name=" + name + ", openstackId=" + openstackId
                + ", protectExternal=" + protectExternal + "]";
    }

    @Override
    public SecurityGroupMemberType getType() {
        return SecurityGroupMemberType.SUBNET;
    }

}
