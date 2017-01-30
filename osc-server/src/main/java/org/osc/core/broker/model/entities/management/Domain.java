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
import org.osc.sdk.manager.element.DomainElement;

@Entity
@Table(name = "DOMAIN")
public class Domain extends BaseEntity implements DomainElement {

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
        return mgrId;
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
        return applianceManagerConnector;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "Domain [name=" + name + ", mgrId=" + mgrId + ", getId()=" + getId() + "]";
    }

}
