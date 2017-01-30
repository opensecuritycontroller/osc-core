package org.osc.core.broker.model.entities.appliance;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.MapKeyColumn;
import javax.persistence.MapKeyEnumerated;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.ForeignKey;
import org.osc.core.broker.model.entities.BaseEntity;
import org.osc.core.broker.model.entities.management.Domain;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupInterface;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.broker.model.entities.virtualization.openstack.OsFlavorReference;
import org.osc.core.broker.model.entities.virtualization.openstack.OsImageReference;
import org.osc.core.broker.model.virtualization.VmwareSoftwareVersion;
import org.osc.sdk.controller.TagEncapsulationType;
import org.osc.sdk.manager.element.VirtualSystemElement;

@SuppressWarnings("serial")
@Entity
@Table(name = "VIRTUAL_SYSTEM", uniqueConstraints = @UniqueConstraint(columnNames = { "virtualization_connector_fk",
"distributed_appliance_fk" }))
public class VirtualSystem extends BaseEntity implements VirtualSystemElement {

    private static final String TEMP_VS_NAME = "~temp~";

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "distributed_appliance_fk", nullable = false)
    @ForeignKey(name = "FK_VS_DISTRIBUTED_APPLIANCE")
    private DistributedAppliance distributedAppliance;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "virtualization_connector_fk", nullable = false)
    @ForeignKey(name = "FK_VS_VIRTUALIZATION_CONNECTOR")
    private VirtualizationConnector virtualizationConnector;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "appliance_sw_version_fk", nullable = false)
    @ForeignKey(name = "FK_VS_APPLIANCE_SW_VERSION")
    private ApplianceSoftwareVersion applianceSoftwareVersion;

    @Column(name = "name", unique = true, nullable = false)
    private String name = TEMP_VS_NAME;

    @Column(name = "key_store")
    @Basic(fetch = FetchType.LAZY)
    @Lob
    private byte[] keyStore = null;

    @Column(name = "nsx_service_manager_id")
    private String nsxServiceManagerId;

    @Column(name = "nsx_service_id")
    private String nsxServiceId;

    @Column(name = "nsx_service_instance_id")
    private String nsxServiceInstanceId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "VIRTUAL_SYSTEM_NSX_DEPLOYMENT_SPEC_ID",
    joinColumns = @JoinColumn(name = "virtual_system_fk"))
    @MapKeyColumn(name="host_version")
    @MapKeyEnumerated(EnumType.STRING)
    @Column(name = "nsx_deployment_spec_id")
    @ForeignKey(name = "FK_VIRTUAL_SYSTEM_NSX_DEPLOYMENT_SPEC_ID")
    private Map<VmwareSoftwareVersion, String> nsxDeploymentSpecIds = new HashMap<>();

    @Column(name = "nsx_vsm_uuid")
    private String nsxVsmUuid;

    @OneToMany(mappedBy = "virtualSystem", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<SecurityGroupInterface> securityGroupInterfaces = new HashSet<SecurityGroupInterface>();

    @OneToMany(mappedBy = "virtualSystem", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<DistributedApplianceInstance> distributedApplianceInstances = new HashSet<DistributedApplianceInstance>();

    @OneToMany(mappedBy = "virtualSystem", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<VirtualSystemPolicy> virtualSystemPolicies = new HashSet<VirtualSystemPolicy>();

    @OneToMany(mappedBy = "virtualSystem", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<VirtualSystemMgrFile> virtualSystemMgrFiles = new HashSet<VirtualSystemMgrFile>();

    @OneToMany(mappedBy = "virtualSystem", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<DeploymentSpec> deploymentSpecs = new HashSet<DeploymentSpec>();

    @OneToMany(mappedBy = "virtualSystem", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<OsImageReference> osImageReference = new HashSet<OsImageReference>();

    @OneToMany(mappedBy = "virtualSystem", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<OsFlavorReference> osFlavorReference = new HashSet<OsFlavorReference>();

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "domain_fk", nullable = true)
    @ForeignKey(name = "FK_AG_DOMAIN")
    private Domain domain;

    @Column(name = "mgr_id")
    private String mgrId;

    @Column(name = "encapsulation_type")
    @Enumerated(EnumType.STRING)
    private TagEncapsulationType encapsulationType;

    public VirtualSystem(DistributedAppliance distributedAppliance) {
        super();

        this.distributedAppliance = distributedAppliance;
    }

    // default constructor is needed for Hibernate
    public VirtualSystem() {
        super();
    }

    @Override
    public byte[] getKeyStore() {
        return this.keyStore;
    }

    public void setKeyStore(byte[] keyStore) {
        this.keyStore = keyStore;
    }

    public Map<VmwareSoftwareVersion, String> getNsxDeploymentSpecIds() {
        return this.nsxDeploymentSpecIds;
    }

    public void setNsxDeploymentSpecIds(Map<VmwareSoftwareVersion, String> nsxDeploymentSpecId) {
        this.nsxDeploymentSpecIds = nsxDeploymentSpecId;
    }

    public String getNsxServiceManagerId() {
        return this.nsxServiceManagerId;
    }

    public void setNsxServiceManagerId(String nsxServiceManagerId) {
        this.nsxServiceManagerId = nsxServiceManagerId;
    }

    public String getNsxServiceId() {
        return this.nsxServiceId;
    }

    public void setNsxServiceId(String nsxServiceId) {
        this.nsxServiceId = nsxServiceId;
    }

    public String getNsxServiceInstanceId() {
        return this.nsxServiceInstanceId;
    }

    public void setNsxServiceInstanceId(String nsxServiceInstanceId) {
        this.nsxServiceInstanceId = nsxServiceInstanceId;
    }

    @Override
    public DistributedAppliance getDistributedAppliance() {
        return this.distributedAppliance;
    }

    void setDistributedAppliance(DistributedAppliance distributedAppliance) {
        this.distributedAppliance = distributedAppliance;
    }

    @Override
    public VirtualizationConnector getVirtualizationConnector() {
        return this.virtualizationConnector;
    }

    public void setVirtualizationConnector(VirtualizationConnector virtualizationConnector) {
        this.virtualizationConnector = virtualizationConnector;
    }

    @Override
    public ApplianceSoftwareVersion getApplianceSoftwareVersion() {
        return this.applianceSoftwareVersion;
    }

    public void setApplianceSoftwareVersion(ApplianceSoftwareVersion applianceSoftwareVersion) {
        this.applianceSoftwareVersion = applianceSoftwareVersion;
    }

    public Set<DistributedApplianceInstance> getDistributedApplianceInstances() {
        return this.distributedApplianceInstances;
    }

    public void addDistributedApplianceInstance(DistributedApplianceInstance distributedApplianceInstance) {
        this.distributedApplianceInstances.add(distributedApplianceInstance);
        distributedApplianceInstance.setVirtualSystem(this);
    }

    public void removeDistributedApplianceInstance(DistributedApplianceInstance distributedApplianceInstance) {
        this.distributedApplianceInstances.remove(distributedApplianceInstance);
    }

    public Set<VirtualSystemMgrFile> getVirtualSystemMgrFiles() {
        return this.virtualSystemMgrFiles;
    }

    public void addVirtualSystemMgrFile(VirtualSystemMgrFile virtualSystemMgrFile) {
        this.virtualSystemMgrFiles.add(virtualSystemMgrFile);
        virtualSystemMgrFile.setVirtualSystem(this);
    }

    public void removeVirtualSystemMgrFile(VirtualSystemMgrFile virtualSystemMgrFile) {
        this.virtualSystemMgrFiles.remove(virtualSystemMgrFile);
    }

    public Set<VirtualSystemPolicy> getVirtualSystemPolicies() {
        return this.virtualSystemPolicies;
    }

    public void addVirtualSystemPolicy(VirtualSystemPolicy virtualSystemPolicy) {
        this.virtualSystemPolicies.add(virtualSystemPolicy);
        virtualSystemPolicy.setVirtualSystem(this);
    }

    public void removeVirtualSystemPolicy(VirtualSystemPolicy virtualSystemPolicy) {
        this.virtualSystemPolicies.remove(virtualSystemPolicy);
    }

    @Override
    public Domain getDomain() {
        return this.domain;
    }

    public void setDomain(Domain domain) {
        this.domain = domain;
    }

    @Override
    public String getMgrId() {
        return this.mgrId;
    }

    public void setMgrId(String mgrId) {
        this.mgrId = mgrId;
    }

    public Set<SecurityGroupInterface> getSecurityGroupInterfaces() {
        return this.securityGroupInterfaces;
    }

    public String getNsxVsmUuid() {
        return this.nsxVsmUuid;
    }

    public void setNsxVsmUuid(String nsxVsmUuid) {
        this.nsxVsmUuid = nsxVsmUuid;
    }

    @Override
    public String getName() {
        if (this.name == null || this.name.equals(TEMP_VS_NAME)) {
            this.name = this.distributedAppliance.getName() + "-" + getId().toString();
        }
        return this.name;
    }

    public Set<DeploymentSpec> getDeploymentSpecs() {
        return this.deploymentSpecs;
    }

    public void setDeploymentSpecs(Set<DeploymentSpec> deploymentSpecs) {
        this.deploymentSpecs = deploymentSpecs;
    }

    public Set<OsImageReference> getOsImageReference() {
        return this.osImageReference;
    }

    public void addOsImageReference(OsImageReference osImageReference) {
        this.osImageReference.add(osImageReference);
    }

    public Set<OsFlavorReference> getOsFlavorReference() {
        return this.osFlavorReference;
    }

    public void addOsFlavorReference(OsFlavorReference osFlavorReference) {
        this.osFlavorReference.add(osFlavorReference);
    }

    public TagEncapsulationType getEncapsulationType() {
        return this.encapsulationType;
    }

    public void setEncapsulationType(TagEncapsulationType encapsulationType) {
        this.encapsulationType = encapsulationType;
    }

    @Override
    public Boolean getMarkedForDeletion() {
        return super.getMarkedForDeletion() || this.distributedAppliance.getMarkedForDeletion();
    }

    /**
     * Given a VS name, tries to get the ID from it. A VS name typically has a ID appended towards the end of it
     * separated by a '-' or a '_'(older versions of ISC uses this).
     *
     * @param vsName vsName containing the ID
     * @return the ID of the virtual system or throws a number format exception
     */
    public static Long getVsIdFromName(String vsName) {
        int underScorePos = vsName.lastIndexOf('_');
        int dashPos = vsName.lastIndexOf('-');
        int pos = Math.max(underScorePos, dashPos);
        return Long.parseLong(vsName.substring(pos + 1));
    }

    @Override
    public String toString() {
        return "VirtualSystem [distributedAppliance=" + this.distributedAppliance.getName()
        + ", virtualizationConnector=" + this.virtualizationConnector.getName()
        + ", applianceSoftwareVersion=" + this.applianceSoftwareVersion
        + ", nsxServiceManagerId=" + this.nsxServiceManagerId
        + ", nsxServiceId=" + this.nsxServiceId + ", nsxServiceInstanceId="
        + this.nsxServiceInstanceId + ", nsxDeploymentSpecId="
        + this.nsxDeploymentSpecIds + ", domain=" + this.domain.getName()
        + ", mgrId=" + this.mgrId + ", getId()=" + getId() + "]";
    }

}
