package org.osc.core.broker.model.entities.virtualization;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
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

import org.apache.commons.lang.StringUtils;
import org.osc.core.broker.model.entities.BaseEntity;
import org.osc.core.broker.model.entities.SslCertificateAttr;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.plugin.sdncontroller.ControllerType;
import org.osc.core.broker.model.virtualization.VirtualizationType;
import org.osc.core.rest.client.crypto.SslContextProvider;
import org.osc.core.rest.client.crypto.X509TrustManagerFactory;

@Entity
@Table(name = "VIRTUALIZATION_CONNECTOR")
public class VirtualizationConnector extends BaseEntity
        implements org.osc.sdk.manager.element.VirtualizationConnectorElement,
        org.osc.sdk.controller.element.VirtualizationConnectorElement {

    private static final long serialVersionUID = 1L;

    public static final String ATTRIBUTE_KEY_HTTPS = "ishttps";
    public static final String ATTRIBUTE_KEY_RABBITMQ_IP = "rabbitMQIP";
    public static final String ATTRIBUTE_KEY_RABBITMQ_USER = "rabbitUser";
    public static final String ATTRIBUTE_KEY_RABBITMQ_USER_PASSWORD = "rabbitMQPassword";
    public static final String ATTRIBUTE_KEY_RABBITMQ_PORT = "rabbitMQPort";

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
    private String controllerType = ControllerType.NONE.getValue();

    @OneToMany(mappedBy = "virtualizationConnector", fetch = FetchType.LAZY)
    private Set<VirtualSystem> virtualSystems = new HashSet<VirtualSystem>();

    @OneToMany(mappedBy = "virtualizationConnector", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<SecurityGroup> securityGroups = new HashSet<SecurityGroup>();

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

    @Column(name = "admin_tenant_name")
    private String adminTenantName;

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
        this.adminTenantName = originalVc.adminTenantName;
    }

    @Override
    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getControllerIpAddress() {
        return this.controllerIpAddress;
    }

    public void setControllerIpAddress(String controllerIpAddress) {
        this.controllerIpAddress = controllerIpAddress;
    }

    @Override
    public String getControllerUsername() {
        return this.controllerUsername;
    }

    public void setControllerUsername(String controllerUsername) {
        this.controllerUsername = controllerUsername;
    }

    @Override
    public String getControllerPassword() {
        return this.controllerPassword;
    }

    public void setControllerPassword(String controllerPassword) {
        this.controllerPassword = controllerPassword;
    }

    @Override
    public String getProviderIpAddress() {
        return this.providerIpAddress;
    }

    public void setProviderIpAddress(String providerIpAddress) {
        this.providerIpAddress = providerIpAddress;
    }

    @Override
    public String getProviderUsername() {
        return this.providerUsername;
    }

    public void setProviderUsername(String providerUsername) {
        this.providerUsername = providerUsername;
    }

    @Override
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

    public ControllerType getControllerType() {
        return ControllerType.fromText(this.controllerType);
    }

    public void setControllerType(ControllerType controllerType) {
        this.controllerType = controllerType != null ? controllerType.getValue() : ControllerType.NONE.getValue();
    }

    public boolean isControllerDefined() {
        return !getControllerType().equals(ControllerType.NONE);
    }

    public boolean isVmware() {
        return getVirtualizationType().isVmware();
    }

    public boolean isOpenstack() {
        return getVirtualizationType().isOpenstack();
    }

    public Set<VirtualSystem> getVirtualSystems() {
        return this.virtualSystems;
    }

    @Override
    public String getProviderAdminTenantName() {
        return this.adminTenantName;
    }

    public void setAdminTenantName(String adminTenantName) {
        this.adminTenantName = adminTenantName;
    }

    public Set<SslCertificateAttr> getSslCertificateAttrSet() {
        return sslCertificateAttrSet;
    }

    public void setSslCertificateAttrSet(Set<SslCertificateAttr> sslCertificateAttrSet) {
        this.sslCertificateAttrSet = sslCertificateAttrSet;
    }

    @Override
    public Map<String, String> getProviderAttributes() {
        return this.providerAttributes;
    }

    @Override
    public SSLContext getSslContext() {
        SslContextProvider sslContextProvider = new SslContextProvider();
        return sslContextProvider.getSSLContext();
    }

    @Override
    public TrustManager[] getTruststoreManager() throws Exception {
        return new TrustManager[]{X509TrustManagerFactory.getInstance()};
    }

    @Override
    public boolean isProviderHttps() {
        String httpsValue = this.providerAttributes.get(VirtualizationConnector.ATTRIBUTE_KEY_HTTPS);
        return httpsValue != null && httpsValue.equals(Boolean.TRUE.toString());
    }

    public static boolean isHttps(Map<String, String> attributes) {
        String httpsValue = attributes.get(VirtualizationConnector.ATTRIBUTE_KEY_HTTPS);

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

    /**
     * Gets the RabbitMQ IP address
     * @return The RabbitMQ IP address from the provider attributes if found, otherwise returns the provider IP address.
     */
    public String getRabbitMQIP() {
        String rabbitMQIP = getProviderAttributes() != null ? getProviderAttributes().get(ATTRIBUTE_KEY_RABBITMQ_IP) : null;
        return StringUtils.isBlank(rabbitMQIP) ? getProviderIpAddress() : rabbitMQIP;
    }

    @Override
    public boolean isControllerHttps() {
        // TODO: Future. Need to add support for Controller HTTPS access
        return false;
    }

}
