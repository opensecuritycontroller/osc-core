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
package org.osc.core.broker.model.entities.management;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.osc.core.broker.model.entities.BaseEntity;
import org.osc.core.broker.model.entities.SslCertificateAttr;
import org.osc.core.broker.model.entities.job.JobRecord;
import org.osc.core.broker.model.entities.job.LastJobContainer;

@SuppressWarnings("serial")
@Entity
@Table(name = "APPLIANCE_MANAGER_CONNECTOR")
public class ApplianceManagerConnector extends BaseEntity implements LastJobContainer {

    @Column(name = "name", unique = true, nullable = false)
    private String name;

    @Column(name = "public_key")
    @Basic(fetch = FetchType.LAZY)
    @Lob
    private byte[] publicKey = null;

    @Column(name = "manager_type", nullable = false)
    private String managerType;

    @Column(name = "service_type", nullable = false)
    private String serviceType;

    @Column(name = "ip_address", unique = true, nullable = false)
    private String ipAddress;

    @Column(name = "username")
    private String username;

    @Column(name = "password")
    private String password;

    @OneToMany(mappedBy = "applianceManagerConnector", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<Policy> policies = new HashSet<Policy>();

    @OneToMany(mappedBy = "applianceManagerConnector", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<Domain> domains = new HashSet<Domain>();

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_job_id_fk", foreignKey = @ForeignKey(name = "FK_MC_LAST_JOB"))
    private JobRecord lastJob;

    @Column(name = "last_known_broker_ip_address", nullable = true)
    private String lastKnownBrokerIpAddress;

    @Column(name = "api_key")
    private String apiKey;

    @Transient
    private String clientIpAddress;

    @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinTable(name="SSL_CERTIFICATE_ATTR_APPL_MAN_CONNECTOR",
    joinColumns={@JoinColumn(name="APPLIANCE_MANAGER_CONNECTOR_ID")},
    inverseJoinColumns={@JoinColumn(name="SSL_CERTIFICATE_ATTR_ID")})
    private Set<SslCertificateAttr> sslCertificateAttrSet = new HashSet<>();

    public ApplianceManagerConnector() {
        super();
    }

    /**
     * Provides a SHALLOW Copy of the Appliance Manager Connector object.
     *
     * @param originalMc
     */
    public ApplianceManagerConnector(ApplianceManagerConnector originalMc) {
        super(originalMc);
        this.name = originalMc.name;
        this.publicKey = originalMc.publicKey;
        this.managerType = originalMc.managerType;
        this.serviceType = originalMc.serviceType;
        this.ipAddress = originalMc.ipAddress;
        this.username = originalMc.username;
        this.password = originalMc.password;
        this.policies = originalMc.policies;
        this.domains = originalMc.domains;
        this.lastJob = originalMc.lastJob;
        this.lastKnownBrokerIpAddress = originalMc.lastKnownBrokerIpAddress;
        this.apiKey = originalMc.apiKey;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getManagerType() {
        return this.managerType;
    }

    public void setManagerType(String managerType) {
        this.managerType = managerType;
    }

    public String getServiceType() {
        return this.serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public String getIpAddress() {
        return this.ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUsername() {
        return this.username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return this.password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Set<Policy> getPolicies() {
        return this.policies;
    }

    public Set<Domain> getDomains() {
        return this.domains;
    }

    public byte[] getPublicKey() {
        return this.publicKey;
    }

    public void setPublicKey(byte[] publicKey) {
        this.publicKey = publicKey;
    }

    public void addPolicy(Policy policy) {
        this.policies.add(policy);
        policy.setApplianceManagerConnector(this);
    }

    public void removePolicy(Policy policy) {
        this.policies.remove(policy);
    }

    public void addDomain(Domain domain) {
        this.domains.add(domain);
        domain.setApplianceManagerConnector(this);
    }

    public void removeDomain(Domain domain) {
        this.domains.remove(domain);
    }

    public String getApiKey() {
        return this.apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public Set<SslCertificateAttr> getSslCertificateAttrSet() {
        return this.sslCertificateAttrSet;
    }

    public void setSslCertificateAttrSet(Set<SslCertificateAttr> sslCertificateAttrSet) {
        this.sslCertificateAttrSet = sslCertificateAttrSet;
    }

    /**
     * Gets the last known ISC notification ip address known by the MC. This is needed so
     * that we can communicate to the MC when our IP changes to update their
     * notification mechanism instead of adding new notification registrations
     * Can be null/empty
     *
     * @return
     */
    public String getLastKnownNotificationIpAddress() {
        return this.lastKnownBrokerIpAddress;
    }

    public void setLastKnownNotificationIpAddress(String lastKnownNotificationIpAddress) {
        this.lastKnownBrokerIpAddress = lastKnownNotificationIpAddress;
    }

    @Override
    public JobRecord getLastJob() {
        return this.lastJob;
    }

    @Override
    public void setLastJob(JobRecord lastJob) {
        this.lastJob = lastJob;
    }

    @Override
    public String toString() {
        return "ApplianceManagerConnector [name=" + this.name + ", managerType=" + this.managerType + ", serviceType="
                + this.serviceType + ", ipAddress=" + this.ipAddress + ", username=" + this.username
                + ", lastKnownIpAddress=" + this.lastKnownBrokerIpAddress + ", getId()=" + getId() + "]";
    }

    /**
     * @see org.osc.sdk.manager.element.ApplianceManagerConnectorElement#getClientIpAddress()
     */
    public String getClientIpAddress() {
        return this.clientIpAddress;
    }

    /**
     * Sets the IP address of the client (OSC) connecting to the appliance manager.
     * @param clientIpAddress the IP address of the client (OSC) connecting to the appliance manager.
     */
    public void setClientIpAddress(String clientIpAddress) {
        this.clientIpAddress = clientIpAddress;
    }
}