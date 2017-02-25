/*******************************************************************************
 * Copyright (c) 2017 Intel Corporation
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
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.ForeignKey;
import org.osc.core.broker.model.entities.BaseEntity;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.appliance.VirtualSystemPolicy;
import org.osc.core.broker.model.entities.management.Policy;
import org.osc.sdk.controller.FailurePolicyType;
import org.osc.sdk.manager.element.ManagerSecurityGroupInterfaceElement;

@SuppressWarnings("serial")
@Entity
@Table(name = "SECURITY_GROUP_INTERFACE", uniqueConstraints = { @UniqueConstraint(columnNames = { "virtual_system_fk",
"tag" }) })
public class SecurityGroupInterface extends BaseEntity implements ManagerSecurityGroupInterfaceElement {

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
    @JoinColumn(name = "virtual_system_fk", nullable = false)
    @ForeignKey(name = "FK_SG_VIRTUAL_SYSTEM")
    private VirtualSystem virtualSystem;

    /**
     * Either the VSP or the policy may be populated. For appliance managers that do not support policy mapping this value
     * will be null. VSP will be set when the controller is handling the policy binding. If ISC is handling the policy binding, the policy
     * should be set.
     */

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "virtual_system_policy_fk")
    @ForeignKey(name = "FK_SG_VIRTUAL_SYSTEM_POLIC")
    private VirtualSystemPolicy virtualSystemPolicy;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "policy_fk")
    @ForeignKey(name = "FK_SGI_POLICY")
    private Policy policy;

    @Column(name = "nsx_vsm_uuid")
    private String nsxVsmUuid;

    /**
     * The tag is assumed to be in the format "SOMESTRING" "-" "LONG VALUE". In case of
     * NSX it will be serviceprofile-456 and in case of a custom isc tag it will be in the format
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

    public SecurityGroupInterface(VirtualSystemPolicy virtualSystemPolicy, String tag) {
        super();
        this.virtualSystemPolicy = virtualSystemPolicy;
        this.virtualSystem = virtualSystemPolicy.getVirtualSystem();
        this.tag = tag;
    }

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

    public VirtualSystemPolicy getVirtualSystemPolicy() {
        return this.virtualSystemPolicy;
    }

    public void setPolicy(Policy policy) {
        this.policy = policy;
    }

    @Override
    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNsxVsmUuid() {
        return this.nsxVsmUuid;
    }

    public void setNsxVsmUuid(String nsxVsmUuid) {
        this.nsxVsmUuid = nsxVsmUuid;
    }

    @Override
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
        if (this.virtualSystemPolicy == null) {
            return this.policy;
        }
        return this.virtualSystemPolicy.getPolicy();
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

    @Override
    public String getSecurityGroupInterfaceId() {
        return this.mgrSecurityGroupIntefaceId;
    }

    @Override
    public String getPolicyId() {
        return getMgrPolicyId();
    }

    public Long getOrder() {
        return this.order;
    }

    public void setOrder(long order) {
        this.order = order;
    }
}
