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
package org.osc.core.broker.model.entities.virtualization;

import static org.osc.core.common.virtualization.VirtualizationConnectorProperties.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.MapKeyColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.osc.core.broker.model.entities.BaseEntity;
import org.osc.core.broker.model.entities.SslCertificateAttr;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.common.virtualization.VirtualizationType;

@Entity
@Table(name = "VIRTUALIZATION_CONNECTOR")
public class VirtualizationConnector extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Column(name = "name", unique = true, nullable = false)
    private String name;

    @Column(name = "controller_ip_address")
    private String controllerIpAddress;

    @Column(name = "controller_username")
    private String controllerUsername;

    @Column(name = "controller_password")
    private String controllerPassword;

    @Column(name = "provider_ip_address", unique = true, nullable = false)
    private String providerIpAddress;

    @Column(name = "provider_username", nullable = false)
    private String providerUsername;

    @Column(name = "provider_password", nullable = false)
    private String providerPassword;

    @Column(name = "virtualization_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private VirtualizationType virtualizationType;

    @Column(name = "virtualization_software_version")
    private String virtualizationSoftwareVersion;

    @Column(name = "controller_type", nullable = false)
    private String controllerType = NO_CONTROLLER_TYPE;

    @OneToMany(mappedBy = "virtualizationConnector", fetch = FetchType.LAZY)
    private Set<VirtualSystem> virtualSystems = new HashSet<>();

    @OneToMany(mappedBy = "virtualizationConnector", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<SecurityGroup> securityGroups = new HashSet<>();

    @OneToMany(mappedBy = "virtualizationConnector", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<ServiceFunctionChain> serviceFunctionChains = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @MapKeyColumn(name = "key")
    @Column(name = "value")
    @CollectionTable(name = "VIRTUALIZATION_CONNECTOR_PROVIDER_ATTR", joinColumns = @JoinColumn(name = "vc_fk"))
    private Map<String, String> providerAttributes = new HashMap<>();

    @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinTable(name="SSL_CERTIFICATE_ATTR_VIRTUALIZATION_CONNECTOR",
    joinColumns={@JoinColumn(name="VIRTUALIZATION_CONNECTOR_ID")},
    inverseJoinColumns={@JoinColumn(name="SSL_CERTIFICATE_ATTR_ID")})
    private Set<SslCertificateAttr> sslCertificateAttrSet = new HashSet<>();

    @Column(name = "admin_project_name")
    private String adminProjectName;

    @Column(name = "admin_domain_id")
    private String adminDomainId;

    public VirtualizationConnector() {
        super();
    }

    /**
     * Provides a SHALLOW Copy of the virtualization connector object.
     *
     * @param originalVc
     */
    public VirtualizationConnector(VirtualizationConnector originalVc) {
        super(originalVc);
        this.name = originalVc.name;
        this.controllerIpAddress = originalVc.controllerIpAddress;
        this.controllerUsername = originalVc.controllerUsername;
        this.controllerPassword = originalVc.controllerPassword;
        this.providerIpAddress = originalVc.providerIpAddress;
        this.providerUsername = originalVc.providerUsername;
        this.providerPassword = originalVc.providerPassword;
        this.virtualizationType = originalVc.virtualizationType;
        this.virtualizationSoftwareVersion = originalVc.virtualizationSoftwareVersion;
        this.controllerType = originalVc.controllerType;
        this.virtualSystems = originalVc.virtualSystems;
        this.securityGroups = originalVc.securityGroups;
        this.providerAttributes = originalVc.providerAttributes;
        this.adminProjectName = originalVc.adminProjectName;
        this.adminDomainId = originalVc.adminDomainId;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getControllerIpAddress() {
        return this.controllerIpAddress;
    }

    public void setControllerIpAddress(String controllerIpAddress) {
        this.controllerIpAddress = controllerIpAddress;
    }

    public String getControllerUsername() {
        return this.controllerUsername;
    }

    public void setControllerUsername(String controllerUsername) {
        this.controllerUsername = controllerUsername;
    }

    public String getControllerPassword() {
        return this.controllerPassword;
    }

    public void setControllerPassword(String controllerPassword) {
        this.controllerPassword = controllerPassword;
    }

    public String getProviderIpAddress() {
        return this.providerIpAddress;
    }

    public void setProviderIpAddress(String providerIpAddress) {
        this.providerIpAddress = providerIpAddress;
    }

    public String getProviderUsername() {
        return this.providerUsername;
    }

    public void setProviderUsername(String providerUsername) {
        this.providerUsername = providerUsername;
    }

    public String getProviderPassword() {
        return this.providerPassword;
    }

    public void setProviderPassword(String providerPassword) {
        this.providerPassword = providerPassword;
    }

    public VirtualizationType getVirtualizationType() {
        return this.virtualizationType;
    }

    public void setVirtualizationType(VirtualizationType virtualizationType) {
        this.virtualizationType = virtualizationType;
    }

    public String getVirtualizationSoftwareVersion() {
        return this.virtualizationSoftwareVersion;
    }

    public void setVirtualizationSoftwareVersion(String virtualizationSoftwareVersion) {
        this.virtualizationSoftwareVersion = virtualizationSoftwareVersion;
    }

    public String getControllerType() {
        return this.controllerType;
    }

    public void setControllerType(String controllerType) {
        this.controllerType = controllerType != null ? controllerType : NO_CONTROLLER_TYPE;
    }

    public boolean isControllerDefined() {
        return !getControllerType().equals(NO_CONTROLLER_TYPE);
    }

    public Set<VirtualSystem> getVirtualSystems() {
        return this.virtualSystems;
    }

    public String getProviderAdminProjectName() {
        return this.adminProjectName;
    }

    public void setAdminProjectName(String adminProjectName) {
        this.adminProjectName = adminProjectName;
    }

    public String getAdminDomainId() {
        return this.adminDomainId;
    }

    public void setAdminDomainId(String adminDomainId) {
        this.adminDomainId = adminDomainId;
    }

    public Set<SslCertificateAttr> getSslCertificateAttrSet() {
        return this.sslCertificateAttrSet;
    }

    public void setSslCertificateAttrSet(Set<SslCertificateAttr> sslCertificateAttrSet) {
        this.sslCertificateAttrSet = sslCertificateAttrSet;
    }

    public Map<String, String> getProviderAttributes() {
        return this.providerAttributes;
    }

    public boolean isProviderHttps() {
        String httpsValue = this.providerAttributes.get(ATTRIBUTE_KEY_HTTPS);
        return httpsValue != null && httpsValue.equals(Boolean.TRUE.toString());
    }

    @Override
    public String toString() {
        return "VirtualizationConnector [name=" + this.name + ", controllerIpAddress=" + this.controllerIpAddress
                + ", controllerUsername=" + this.controllerUsername + ", providerIpAddress=" + this.providerIpAddress
                + ", providerUsername=" + this.providerUsername + ", virtualizationType=" + this.virtualizationType
                + ", virtualizationSoftwareVersion=" + this.virtualizationSoftwareVersion + ", controllerType="
                + this.controllerType + "]";
    }

    public Set<SecurityGroup> getSecurityGroups() {
        return this.securityGroups;
    }

    public Set<ServiceFunctionChain> getServiceFunctionChains() {
        return this.serviceFunctionChains;
    }

    /**
     * Gets the RabbitMQ IP address
     * @return The RabbitMQ IP address from the provider attributes if found, otherwise returns the provider IP address.
     */
    public String getRabbitMQIP() {
        String rabbitMQIP = getProviderAttributes() != null ? getProviderAttributes().get(ATTRIBUTE_KEY_RABBITMQ_IP) : null;
        return rabbitMQIP == null || rabbitMQIP.isEmpty() ? getProviderIpAddress() : rabbitMQIP;
    }
}