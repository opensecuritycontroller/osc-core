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
