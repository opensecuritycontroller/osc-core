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

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.osc.core.broker.model.entities.BaseEntity;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.management.Policy;

@SuppressWarnings("serial")
@Entity
@Table(name = "SECURITY_GROUP_INTERFACE", uniqueConstraints = { @UniqueConstraint(columnNames = { "virtual_system_fk",
"tag" }) })
public class SecurityGroupInterface extends BaseEntity {

    public static class SecurityGroupInterfaceOrderComparator implements Comparator<SecurityGroupInterface> {

        @Override
        public int compare(SecurityGroupInterface o1, SecurityGroupInterface o2) {
            return Long.compare(o1.getOrder(), o2.getOrder());
        }
    }

    public static final String ISC_TAG_PREFIX = "isc-";

    @Column(name = "name", nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "virtual_system_fk", nullable = false,
    foreignKey = @ForeignKey(name = "FK_SG_VIRTUAL_SYSTEM"))
    private VirtualSystem virtualSystem;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "policy_fk", nullable = false,
        foreignKey = @ForeignKey(name = "FK_SGI_POLICY"))
    private Policy policy;

    /**
     * The tag is assumed to be in the format "SOMESTRING" "-" "LONG VALUE".
     * isc-456 for example.
     */
    @Column(name = "tag", nullable = true)
    private String tag;

    @ManyToMany(fetch = FetchType.LAZY, mappedBy = "securityGroupInterfaces")
    private Set<SecurityGroup> securityGroups = new HashSet<SecurityGroup>();

    @Column(name = "user_configurable", columnDefinition = "bit default 0")
    private boolean isUserConfigurable;

    @Column(name = "mgr_interface_id")
    private String mgrSecurityGroupIntefaceId;

    @Column(name = "failure_policy_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private FailurePolicyType failurePolicyType = FailurePolicyType.NA;

    @Column(name = "chain_order", columnDefinition = "bigint default 0", nullable = false)
    private Long order = 0L;

    /**
     * Represents the identifier of the inspection hook created in the SDN controller
     * when it supports port groups.
     */
    @Column(name = "network_elem_id")
    private String networkElementId;

    public SecurityGroupInterface(VirtualSystem virtualSystem, Policy policy, String tag,
            FailurePolicyType failurePolicyType, Long order) {
        super();
        this.virtualSystem = virtualSystem;
        this.policy = policy;
        this.tag = tag;
        this.isUserConfigurable = true;
        this.failurePolicyType = failurePolicyType;
        this.order = order;
    }

    public SecurityGroupInterface() {
        super();
    }

    public Policy getPolicy() {
        return this.policy;
    }

    public void setPolicy(Policy policy) {
        this.policy = policy;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTag() {
        return this.tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public Long getTagValue() {
        return this.tag == null ? null : Long.valueOf(this.tag.substring(this.tag.indexOf("-") + 1));
    }

    public String getMgrPolicyId() {
        return getMgrPolicy().getMgrPolicyId();
    }

    public Policy getMgrPolicy() {
        return this.policy;
    }

    public VirtualSystem getVirtualSystem() {
        return this.virtualSystem;
    }

    public void setUserConfigurable(boolean isUserConfigurable) {
        this.isUserConfigurable = isUserConfigurable;
    }

    public boolean isUserConfigurable() {
        return this.isUserConfigurable;
    }

    public String getMgrSecurityGroupIntefaceId() {
        return this.mgrSecurityGroupIntefaceId;
    }

    public void setMgrSecurityGroupIntefaceId(String mgrSecurityGroupIntefaceId) {
        this.mgrSecurityGroupIntefaceId = mgrSecurityGroupIntefaceId;
    }

    public FailurePolicyType getFailurePolicyType() {
        return this.failurePolicyType;
    }

    public void setFailurePolicyType(FailurePolicyType failurePolicyType) {
        this.failurePolicyType = failurePolicyType;
    }

    void setSecurityGroupNetworkInterfaces(Set<SecurityGroup> securityGroups) {
        this.securityGroups = securityGroups;
    }

    public Set<SecurityGroup> getSecurityGroups() {
        return this.securityGroups;
    }

    public void addSecurityGroup(SecurityGroup securityGroup) {
        this.securityGroups.add(securityGroup);
    }

    public void removeSecurity(SecurityGroup securityGroup) {
        this.securityGroups.remove(securityGroup);
    }

    public SecurityGroup getSecurityGroup() {
        // TODO: Future. Need to figure out how to eliminate this function and callers since SG to SGI
        // has many to many relationship.
        if (!this.securityGroups.isEmpty()) {
            return this.securityGroups.iterator().next();
        }
        return null;
    }

    public String getSecurityGroupInterfaceId() {
        return this.mgrSecurityGroupIntefaceId;
    }

    public String getPolicyId() {
        return getMgrPolicyId();
    }

    public Long getOrder() {
        return this.order;
    }

    public void setOrder(long order) {
        this.order = order;
    }

    public String getNetworkElementId() {
        return this.networkElementId;
    }

    public void setNetworkElementId(String networkElemId) {
        this.networkElementId = networkElemId;
    }
}
