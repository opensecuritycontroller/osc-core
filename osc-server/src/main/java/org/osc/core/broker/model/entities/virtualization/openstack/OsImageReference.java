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
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.ForeignKey;
import org.osc.core.broker.model.entities.BaseEntity;
import org.osc.core.broker.model.entities.appliance.ApplianceSoftwareVersion;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;

@Entity
@Table(name = "OS_IMAGE_REFERENCE")
public class OsImageReference extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Column(name = "region", nullable = false)
    private String region;

    @Column(name = "image_ref_id", nullable = false, unique = true)
    private String imageRefId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "vs_fk", nullable = false)
    @ForeignKey(name = "FK_VS_OS_IMAGE_REFERENCE")
    private VirtualSystem virtualSystem;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "asv_fk")
    @ForeignKey(name = "FK_ASV_OS_IMAGE_REFERENCE")
    private ApplianceSoftwareVersion applianceVersion;

    OsImageReference() {

    }

    public OsImageReference(VirtualSystem virtualSystem, String region, String imageRefId) {
        this.virtualSystem = virtualSystem;
        this.region = region;
        this.imageRefId = imageRefId;
        this.applianceVersion = virtualSystem.getApplianceSoftwareVersion();
    }

    public VirtualSystem getVirtualSystem() {
        return this.virtualSystem;
    }

    public String getRegion() {
        return this.region;
    }

    public String getImageRefId() {
        return this.imageRefId;
    }

    public ApplianceSoftwareVersion getApplianceVersion() {
        return this.applianceVersion;
    }

    public void setApplianceVersion(ApplianceSoftwareVersion applianceVersion) {
        this.applianceVersion = applianceVersion;
    }

}
