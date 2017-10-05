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
package org.osc.core.broker.model.entities.appliance;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.osc.core.broker.model.entities.BaseEntity;
import org.osc.core.broker.model.entities.management.Domain;
import org.osc.core.broker.model.entities.virtualization.SecurityGroupInterface;
import org.osc.core.broker.model.entities.virtualization.ServiceFunctionChain;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.broker.model.entities.virtualization.openstack.OsFlavorReference;
import org.osc.core.broker.model.entities.virtualization.openstack.OsImageReference;

@SuppressWarnings("serial")
@Entity
@Table(name = "VIRTUAL_SYSTEM", uniqueConstraints = @UniqueConstraint(columnNames = { "virtualization_connector_fk",
"distributed_appliance_fk" }))
public class VirtualSystem extends BaseEntity {

    private static final String TEMP_VS_NAME = "~temp~";

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "distributed_appliance_fk", nullable = false,
            foreignKey = @ForeignKey(name = "FK_VS_DISTRIBUTED_APPLIANCE"))

    private DistributedAppliance distributedAppliance;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "virtualization_connector_fk", nullable = false,
            foreignKey = @ForeignKey(name = "FK_VS_VIRTUALIZATION_CONNECTOR"))
    private VirtualizationConnector virtualizationConnector;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "appliance_sw_version_fk", nullable = false,
            foreignKey = @ForeignKey(name = "FK_VS_APPLIANCE_SW_VERSION"))
    private ApplianceSoftwareVersion applianceSoftwareVersion;

    @Column(name = "name", unique = true, nullable = false)
    private String name = TEMP_VS_NAME;

    @Column(name = "key_store")
    @Basic(fetch = FetchType.LAZY)
    @Lob
    private byte[] keyStore = null;

    @OneToMany(mappedBy = "virtualSystem", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<SecurityGroupInterface> securityGroupInterfaces = new HashSet<SecurityGroupInterface>();

    @OneToMany(mappedBy = "virtualSystem", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<DistributedApplianceInstance> distributedApplianceInstances = new HashSet<DistributedApplianceInstance>();

    @OneToMany(mappedBy = "virtualSystem", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<DeploymentSpec> deploymentSpecs = new HashSet<DeploymentSpec>();

    @OneToMany(mappedBy = "virtualSystem", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<OsImageReference> osImageReference = new HashSet<OsImageReference>();

    @OneToMany(mappedBy = "virtualSystem", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<OsFlavorReference> osFlavorReference = new HashSet<OsFlavorReference>();

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "domain_fk", nullable = true,
            foreignKey = @ForeignKey(name = "FK_AG_DOMAIN"))
    private Domain domain;

    @Column(name = "mgr_id")
    private String mgrId;

    @Column(name = "encapsulation_type")
    @Enumerated(EnumType.STRING)
    private TagEncapsulationType encapsulationType;
    
    @ManyToMany(fetch = FetchType.LAZY, mappedBy = "virtualSystems")
    private List<ServiceFunctionChain> serviceFunctionChains = new ArrayList<ServiceFunctionChain>();

    public VirtualSystem(DistributedAppliance distributedAppliance) {
        super();

        this.distributedAppliance = distributedAppliance;
    }

    // default constructor is needed for Hibernate
    public VirtualSystem() {
        super();
    }

    public byte[] getKeyStore() {
        return this.keyStore;
    }

    public void setKeyStore(byte[] keyStore) {
        this.keyStore = keyStore;
    }

    public DistributedAppliance getDistributedAppliance() {
        return this.distributedAppliance;
    }

    public void setDistributedAppliance(DistributedAppliance distributedAppliance) {
        this.distributedAppliance = distributedAppliance;
    }

    public VirtualizationConnector getVirtualizationConnector() {
        return this.virtualizationConnector;
    }

    public void setVirtualizationConnector(VirtualizationConnector virtualizationConnector) {
        this.virtualizationConnector = virtualizationConnector;
    }

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

    public Domain getDomain() {
        return this.domain;
    }

    public void setDomain(Domain domain) {
        this.domain = domain;
    }

    public String getMgrId() {
        return this.mgrId;
    }

    public void setMgrId(String mgrId) {
        this.mgrId = mgrId;
    }

	public Set<SecurityGroupInterface> getSecurityGroupInterfaces() {
		return this.securityGroupInterfaces;
	}

    public void setSecurityGroupInterfaces(Set<SecurityGroupInterface> securityGroupInterfaces) {
        this.securityGroupInterfaces = securityGroupInterfaces;
    }

    public String getName() {
        if (this.name == null || this.name.equals(TEMP_VS_NAME)) {
            this.name = this.distributedAppliance.getName() + "-" + getId().toString();
        }
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
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

    public void removeOsImageReference(OsImageReference osImageReference) {
        this.osImageReference.remove(osImageReference);
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
    
    public List<ServiceFunctionChain> getServiceFunctionChains() {
        return serviceFunctionChains;
    }

    public void setServiceFunctionChains(List<ServiceFunctionChain> serviceFunctionChains) {
        this.serviceFunctionChains = serviceFunctionChains;
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
        + ", domain=" + this.domain.getName()
        + ", mgrId=" + this.mgrId + ", getId()=" + getId() + "]";
    }

}
