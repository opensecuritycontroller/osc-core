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
package org.osc.core.broker.model.entities.appliance;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.ForeignKey;
import org.osc.core.broker.model.entities.BaseEntity;
import org.osc.core.broker.model.entities.management.Policy;

@Entity
@Table(name = "VIRTUAL_SYSTEM_POLICY", uniqueConstraints = @UniqueConstraint(columnNames = { "vs_fk", "policy_fk" }))
public class VirtualSystemPolicy extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "vs_fk", nullable = false)
    @ForeignKey(name = "FK_VSP_VS")
    private VirtualSystem virtualSystem;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "policy_fk", nullable = false)
    @ForeignKey(name = "FK_VSP_POLICY")
    private Policy policy;

    @Column(name = "nsx_vendor_template_id")
    private String nsxVendorTemplateId;

    public VirtualSystemPolicy() {
        super();
    }

    public VirtualSystemPolicy(VirtualSystem vs) {
        super();

        this.virtualSystem = vs;
    }

    public VirtualSystem getVirtualSystem() {
        return virtualSystem;
    }

    void setVirtualSystem(VirtualSystem virtualSystem) {
        this.virtualSystem = virtualSystem;
    }

    public String getNsxVendorTemplateId() {
        return nsxVendorTemplateId;
    }

    public void setNsxVendorTemplateId(String nsxVendorTemplateId) {
        this.nsxVendorTemplateId = nsxVendorTemplateId;
    }

    public Policy getPolicy() {
        return policy;
    }

    public void setPolicy(Policy policy) {
        this.policy = policy;
    }

    @Override
    public String toString() {
        return "VirtualSystemPolicy [policy=" + policy + ", nsxVendorTemplateId=" + nsxVendorTemplateId + ", getId()="
                + getId() + "]";
    }
}
