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
