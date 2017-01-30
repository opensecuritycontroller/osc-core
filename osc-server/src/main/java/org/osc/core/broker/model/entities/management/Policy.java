package org.osc.core.broker.model.entities.management;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

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
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ApplianceManagerConnector getApplianceManagerConnector() {
        return applianceManagerConnector;
    }

    void setApplianceManagerConnector(ApplianceManagerConnector applianceManagerConnector) {
        this.applianceManagerConnector = applianceManagerConnector;
    }

    public Domain getDomain() {
        return domain;
    }

    public void setDomain(Domain domain) {
        this.domain = domain;
    }

    public String getMgrPolicyId() {
        return mgrPolicyId;
    }

    public void setMgrPolicyId(String mgrPolicyId) {
        this.mgrPolicyId = mgrPolicyId;
    }

    @Override
    public String toString() {
        return "Policy [name=" + name + ", applianceManagerConnector=" + applianceManagerConnector + ", mgrPolicyId="
                + mgrPolicyId + ", getId()=" + getId() + "]";
    }

}
