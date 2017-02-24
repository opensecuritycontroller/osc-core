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
package org.osc.core.broker.model.entities.management;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.hibernate.annotations.ForeignKey;
import org.osc.core.broker.model.entities.BaseEntity;

@Entity
@Table(name = "POLICY")
public class Policy extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Column(name = "name", nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "appliance_manager_connector_fk", nullable = false)
    @ForeignKey(name = "FK_PO_APPLIANCE_MANAGER_CONNECTOR")
    // name our own index
    private ApplianceManagerConnector applianceManagerConnector;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "domain_fk", nullable = false)
    @ForeignKey(name = "FK_PO_DOMAIN")
    private Domain domain;

    @Column(name = "mgr_policy_id", nullable = false)
    private String mgrPolicyId;

    public Policy() {
        super();
    }

    public Policy(ApplianceManagerConnector applianceManagerConnector, Domain domain) {
        super();

        this.applianceManagerConnector = applianceManagerConnector;
        this.domain = domain;
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

    void setApplianceManagerConnector(ApplianceManagerConnector applianceManagerConnector) {
        this.applianceManagerConnector = applianceManagerConnector;
    }

    public Domain getDomain() {
        return this.domain;
    }

    public void setDomain(Domain domain) {
        this.domain = domain;
    }

    public String getMgrPolicyId() {
        return this.mgrPolicyId;
    }

    public void setMgrPolicyId(String mgrPolicyId) {
        this.mgrPolicyId = mgrPolicyId;
    }

    @Override
    public String toString() {
        return "Policy [name=" + this.name + ", applianceManagerConnector=" + this.applianceManagerConnector + ", mgrPolicyId="
                + this.mgrPolicyId + ", getId()=" + getId() + "]";
    }

    @Override
    public int hashCode() {
        HashCodeBuilder builder = new HashCodeBuilder()
                .append(getName())
                .append(getId());
        if (this.applianceManagerConnector != null) {
            builder.append(this.applianceManagerConnector.getId());
        }

        return builder.toHashCode();
    }

    @Override
    public boolean equals(Object object) {
        if (object == null) {
            return false;
        }
        if (getClass() != object.getClass()) {
            return false;
        }
        if (this == object) {
            return true;
        }

        Policy other = (Policy) object;
        EqualsBuilder builder =  new EqualsBuilder()
                .append(getName(), other.getName())
                .append(getId(), other.getId());

        Long mcId = this.applianceManagerConnector != null ? this.applianceManagerConnector.getId() : null;
        Long otherMcId = other.getApplianceManagerConnector() != null ? other.getApplianceManagerConnector().getId() : null;
        builder.append(mcId, otherMcId);

        return  builder.isEquals();
    }

}
