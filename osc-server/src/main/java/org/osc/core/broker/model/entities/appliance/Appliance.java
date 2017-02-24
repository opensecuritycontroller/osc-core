package org.osc.core.broker.model.entities.appliance;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.osc.core.broker.model.entities.BaseEntity;
import org.osc.core.broker.model.plugin.manager.ManagerType;
import org.osc.sdk.manager.element.ApplianceElement;

@Entity
@Table(name = "APPLIANCE")
public class Appliance extends BaseEntity implements ApplianceElement {

    private static final long serialVersionUID = 1L;

    @Column(name = "model", unique = true, nullable = false)
    private String model;

    @Column(name = "manager_type", nullable = false)
    private String managerType;

    @Column(name = "manager_software_version", nullable = false)
    private String managerSoftwareVersion;

    @OneToMany(mappedBy = "appliance", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<ApplianceSoftwareVersion> applianceSoftwareVersions = new HashSet<ApplianceSoftwareVersion>();

    public Appliance() {
        super();
    }

    public void addApplianceSoftwareVersion(ApplianceSoftwareVersion applianceSoftwareVersion) {
        applianceSoftwareVersions.add(applianceSoftwareVersion);
        applianceSoftwareVersion.setAppliance(this);
    }

    public void removeApplianceSoftwareVersion(ApplianceSoftwareVersion applianceSoftwareVersion) {
        applianceSoftwareVersions.remove(applianceSoftwareVersion);
    }

    public String getManagerSoftwareVersion() {
        return managerSoftwareVersion;
    }

    public void setManagerSoftwareVersion(String managerSoftwareVersion) {
        this.managerSoftwareVersion = managerSoftwareVersion;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    @Override
    public String toString() {
        return "Appliance [model=" + model + ", managerType=" + managerType + ", managerSoftwareVersion="
                + managerSoftwareVersion + ", getId()=" + getId() + "]";
    }

    public ManagerType getManagerType() {
        return ManagerType.fromText(managerType);
    }

    public void setManagerType(ManagerType managerType) {
        this.managerType = managerType.getValue();
    }

    public Set<ApplianceSoftwareVersion> getApplianceVersions() {
        return applianceSoftwareVersions;
    }

}
