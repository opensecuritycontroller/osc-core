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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.osc.core.broker.model.entities.BaseEntity;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;

@Entity
@Table(name = "OS_FLAVOR_REFERENCE")
public class OsFlavorReference extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Column(name = "region", nullable = false)
    private String region;

    @Column(name = "flavor_ref_id", nullable = false, unique = true)
    private String flavorRefId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "vs_fk", nullable = false,
            foreignKey = @ForeignKey(name = "FK_OS_FLAVOR_REFERENCE_VS"))
    private VirtualSystem virtualSystem;

    OsFlavorReference() {

    }

    public OsFlavorReference(VirtualSystem virtualSystem, String region, String flavorRefId) {
        this.virtualSystem = virtualSystem;
        this.region = region;
        this.flavorRefId = flavorRefId;
    }

    public VirtualSystem getVirtualSystem() {
        return this.virtualSystem;
    }

    public String getRegion() {
        return this.region;
    }

    public String getFlavorRefId() {
        return this.flavorRefId;
    }
}
