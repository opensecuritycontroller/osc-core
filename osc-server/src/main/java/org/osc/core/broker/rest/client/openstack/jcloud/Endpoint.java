package org.osc.core.broker.rest.client.openstack.jcloud;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.util.EncryptionUtil;

import javax.net.ssl.SSLContext;

public class Endpoint {

    private String endPointIP;
    private String tenant;
    private String user;
    private String password;
    private boolean isHttps;
    private SSLContext sslContext;

    public Endpoint(String endPointIP, String tenant, String user, String password, boolean isHttps, SSLContext sslContext) {
        this.endPointIP = endPointIP;
        this.tenant = tenant;
        this.user = user;
        this.password = password;
        this.isHttps = isHttps;
        this.sslContext = sslContext;
    }

    public Endpoint(VirtualizationConnector vc) {
        this.endPointIP = vc.getProviderIpAddress();
        this.tenant = vc.getProviderAdminTenantName();
        this.user = vc.getProviderUsername();
        this.password = EncryptionUtil.decryptAESCTR(vc.getProviderPassword());
        this.isHttps = vc.isProviderHttps();
        this.sslContext = vc.getSslContext();
    }

    public Endpoint(DeploymentSpec ds) {
        this(ds.getVirtualSystem().getVirtualizationConnector(), ds.getTenantName());
    }

    public Endpoint(VirtualizationConnector vc, String tenant) {
        this.endPointIP = vc.getProviderIpAddress();
        this.tenant = tenant;
        this.user = vc.getProviderUsername();
        this.password = EncryptionUtil.decryptAESCTR(vc.getProviderPassword());
        this.isHttps = vc.isProviderHttps();
    }

    public String getEndPointIP() {
        return this.endPointIP;
    }

    public void setEndPointIP(String endPointIP) {
        this.endPointIP = endPointIP;
    }

    public String getTenant() {
        return this.tenant;
    }

    public void setTenant(String tenant) {
        this.tenant = tenant;
    }

    public String getUser() {
        return this.user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return this.password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isHttps() {
        return this.isHttps;
    }

    public SSLContext getSslContext() {
        return sslContext;
    }

    @Override
    public boolean equals(Object object) {
        if (object == null) {
            return false;
        }
        if (getClass() != object.getClass()) {
            return false;
        }
        if (this == object) {
            return true;
        }

        Endpoint other = (Endpoint) object;

        return new EqualsBuilder()
                .append(this.getEndPointIP(), other.getEndPointIP())
                .append(this.getTenant(), other.getTenant())
                .append(this.getUser(), other.getUser())
                .append(this.getPassword(), other.getPassword())
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(this.getEndPointIP())
                .append(this.getTenant())
                .append(this.getUser())
                .append(this.getPassword())
                .toHashCode();
    }
}
