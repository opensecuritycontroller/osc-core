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
@Table(name = "OS_NETWORK")
public class Network extends BaseEntity implements OsProtectionEntity {

    @Column(name = "region", nullable = false)
    private String region;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "openstack_id", nullable = false, unique = true)
    private String openstackId;

    @OneToMany(mappedBy = "network", fetch = FetchType.LAZY)
    private Set<VMPort> ports = new HashSet<>();

    @OneToMany(mappedBy = "network", fetch = FetchType.LAZY)
    private Set<SecurityGroupMember> securityGroupMembers = new HashSet<>();

    public Network(String region, String openstackId, String name) {
        super();
        this.region = region;
        this.openstackId = openstackId;
        this.name = name;
    }

    Network() {
    }

    @Override
    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getRegion() {
        return this.region;
    }

    @Override
    public String getOpenstackId() {
        return this.openstackId;
    }

    public Set<VMPort> getPorts() {
        return this.ports;
    }

    public void setPorts(Set<VMPort> protectedPorts) {
        this.ports = protectedPorts;
    }

    @Override
    public Set<SecurityGroupMember> getSecurityGroupMembers() {
        return this.securityGroupMembers;
    }

    public void setSecurityGroupMembers(Set<SecurityGroupMember> securityGroupMembers) {
        this.securityGroupMembers = securityGroupMembers;
    }

    @Override
    public SecurityGroupMemberType getType() {
        return SecurityGroupMemberType.NETWORK;
    }

    public void addPort(VMPort vmPort) {
        this.ports.add(vmPort);
    }

    public void removePort(VMPort vmPort) {
        this.ports.remove(vmPort);
    }

}
