package org.osc.core.broker.model.entities.virtualization.openstack;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.ForeignKey;
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
    @JoinColumn(name = "vs_fk", nullable = false)
    @ForeignKey(name = "FK_OS_FLAVOR_REFERENCE_VS")
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
