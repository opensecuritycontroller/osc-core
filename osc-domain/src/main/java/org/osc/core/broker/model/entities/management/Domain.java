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
package org.osc.core.broker.model.entities.management;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.annotations.ForeignKey;
import org.osc.core.broker.model.entities.BaseEntity;

@Entity
@Table(name = "DOMAIN")
public class Domain extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Column(name = "name", nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "appliance_manager_connector_fk", nullable = false)
    @ForeignKey(name = "FK_DO_APPLIANCE_MANAGER_CONNECTOR")
    private ApplianceManagerConnector applianceManagerConnector;

    @OneToMany(mappedBy = "domain", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<Policy> policies = new HashSet<Policy>();

    @Column(name = "mgr_id")
    private String mgrId;

    public Domain() {
        super();
    }

    public Domain(ApplianceManagerConnector applianceManagerConnector) {
        super();

        this.applianceManagerConnector = applianceManagerConnector;
    }

    public String getMgrId() {
        return this.mgrId;
    }

    void setApplianceManagerConnector(ApplianceManagerConnector applianceManagerConnector) {
        this.applianceManagerConnector = applianceManagerConnector;
    }

    public Set<Policy> getPolicies() {
        return this.policies;
    }

    public void addPolicy(Policy policy) {
        this.policies.add(policy);
        policy.setDomain(this);
    }

    public void removePolicy(Policy policy) {
        this.policies.remove(policy);
    }

    public void setMgrId(String mgrId) {
        this.mgrId = mgrId;
    }

    public ApplianceManagerConnector getApplianceManagerConnector() {
        return this.applianceManagerConnector;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "Domain [name=" + this.name + ", mgrId=" + this.mgrId + ", getId()=" + getId() + "]";
    }

}
