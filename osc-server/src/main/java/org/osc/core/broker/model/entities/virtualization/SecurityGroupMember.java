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

import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.ForeignKey;
import org.osc.core.broker.model.entities.BaseEntity;
import org.osc.core.broker.model.entities.virtualization.openstack.Network;
import org.osc.core.broker.model.entities.virtualization.openstack.OsProtectionEntity;
import org.osc.core.broker.model.entities.virtualization.openstack.Subnet;
import org.osc.core.broker.model.entities.virtualization.openstack.VM;
import org.osc.core.broker.model.entities.virtualization.openstack.VMPort;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;

@SuppressWarnings("serial")
@Entity
@Table(name = "SECURITY_GROUP_MEMBER", uniqueConstraints = { @UniqueConstraint(columnNames = { "security_group_fk",
        "vm_fk", "network_fk", "address" }) })
public class SecurityGroupMember extends BaseEntity implements Comparable<SecurityGroupMember> {

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "security_group_fk", nullable = false)
    @ForeignKey(name = "FK_SGM_SG")
    private SecurityGroup securityGroup;

    @Column(name = "member_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private SecurityGroupMemberType type;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "vm_fk")
    @ForeignKey(name = "FK_SGM_VM")
    private VM vm;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "network_fk")
    @ForeignKey(name = "FK_SGM_NETWORK")
    private Network network;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "subnet_fk")
    @ForeignKey(name = "FK_SGM_SUBNET")
    private Subnet subnet;

    @Column(name = "address")
    private String address;

    public SecurityGroupMember(SecurityGroup securityGroup, OsProtectionEntity entity) {
        this.securityGroup = securityGroup;
        this.type = entity.getType();
        if (this.type == SecurityGroupMemberType.VM) {
            this.vm = (VM) entity;
        } else if (this.type == SecurityGroupMemberType.NETWORK) {
            this.network = (Network) entity;
        } else if (this.type == SecurityGroupMemberType.SUBNET) {
            this.subnet = (Subnet) entity;
        } else {
            throw new IllegalArgumentException("Protected Entity can only be a VM, a Network or a Subnet in openstack");
        }
        this.securityGroup.addSecurityGroupMember(this);
    }

    public SecurityGroupMember(SecurityGroup securityGroup, SecurityGroupMemberType type, String address) {
        super();
        this.securityGroup = securityGroup;
        this.address = address;
        this.type = type;
        this.securityGroup.addSecurityGroupMember(this);
    }

    SecurityGroupMember() {
    }

    public VM getVm() {
        return this.vm;
    }

    public SecurityGroup getSecurityGroup() {
        return this.securityGroup;
    }

    public SecurityGroupMemberType getType() {
        return this.type;
    }

    public Network getNetwork() {
        return this.network;
    }

    public String getAddress() {
        return this.address;
    }

    public Subnet getSubnet() {
        return this.subnet;
    }

    public String getMemberName() {
        switch (getType()) {
        case VM:
            return getVm().getName();
        case NETWORK:
            return getNetwork().getName();
        case SUBNET:
            return getSubnet().getName();
        case IP:
        case MAC:
            return getAddress();
        default:
            return null;
        }
    }

    public String getMemberRegion() throws VmidcBrokerValidationException {
        switch (getType()) {
        case VM:
            return getVm().getRegion();
        case NETWORK:
            return getNetwork().getRegion();
        case SUBNET:
            return getSubnet().getRegion();
        default:
            throw new VmidcBrokerValidationException("Openstack Id is not applicable for Members of type '" + getType()
                    + "'");
        }
    }

    public String getMemberOpenstackId() throws VmidcBrokerValidationException {
        switch (getType()) {
        case VM:
            return getVm().getOpenstackId();
        case NETWORK:
            return getNetwork().getOpenstackId();
        case SUBNET:
            return getSubnet().getOpenstackId();
        default:
            throw new VmidcBrokerValidationException("Region is not applicable for Members of type '" + getType() + "'");
        }
    }

    public Set<VMPort> getPorts() throws VmidcBrokerValidationException {
        switch (getType()) {
        case VM:
            return getVm().getPorts();
        case NETWORK:
            return getNetwork().getPorts();
        case SUBNET:
            return getSubnet().getPorts();
        default:
            throw new VmidcBrokerValidationException("Region is not applicable for Members of type '" + getType() + "'");
        }
    }


    @Override
    public int compareTo(SecurityGroupMember o) {
        return this.type.compareTo(o.getType());
    }

}
