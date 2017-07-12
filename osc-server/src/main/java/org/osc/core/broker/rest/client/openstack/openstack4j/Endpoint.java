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
package org.osc.core.broker.rest.client.openstack.openstack4j;

import javax.net.ssl.SSLContext;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.broker.service.api.server.EncryptionException;
import org.osc.core.broker.util.StaticRegistry;
import org.osc.core.broker.util.crypto.SslContextProvider;

public class Endpoint {

    private String endPointIP;
    private String domainId;
    private String project;
    private String user;
    private String password;
    private boolean isHttps;
    private SSLContext sslContext;

    public Endpoint(String endPointIP, String domainId, String project, String user, String password, boolean isHttps, SSLContext sslContext) {
        this.endPointIP = endPointIP;
        this.domainId = domainId;
        this.project = project;
        this.user = user;
        this.password = password;
        this.isHttps = isHttps;
        this.sslContext = sslContext;
    }

    public Endpoint(VirtualizationConnector vc) throws EncryptionException {
        this.endPointIP = vc.getProviderIpAddress();
        this.domainId = vc.getAdminDomainId();
        this.project = vc.getProviderAdminProjectName();
        this.user = vc.getProviderUsername();
        this.password = StaticRegistry.encryptionApi().decryptAESCTR(vc.getProviderPassword());
        this.isHttps = vc.isProviderHttps();
        this.sslContext = SslContextProvider.getInstance().getSSLContext();
    }

    public Endpoint(DeploymentSpec ds) throws EncryptionException {
        this(ds.getVirtualSystem().getVirtualizationConnector(), ds.getProjectName());
    }

    public Endpoint(VirtualizationConnector vc, String project) throws EncryptionException {
        this.endPointIP = vc.getProviderIpAddress();
        this.domainId = vc.getAdminDomainId();
        this.project = project;
        this.user = vc.getProviderUsername();
        this.password = StaticRegistry.encryptionApi().decryptAESCTR(vc.getProviderPassword());
        this.isHttps = vc.isProviderHttps();
        this.sslContext = SslContextProvider.getInstance().getSSLContext();
    }

    public String getEndPointIP() {
        return this.endPointIP;
    }

    public void setEndPointIP(String endPointIP) {
        this.endPointIP = endPointIP;
    }

    public String getProject() {
        return this.project;
    }

    public void setProject(String project) {
        this.project = project;
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

    public String getDomainId() {
        return this.domainId;
    }

    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    public boolean isHttps() {
        return this.isHttps;
    }

    public SSLContext getSslContext() {
        return this.sslContext;
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
                .append(getEndPointIP(), other.getEndPointIP())
                .append(getDomainId(), other.getDomainId())
                .append(getProject(), other.getProject())
                .append(getUser(), other.getUser())
                .append(getPassword(), other.getPassword())
                .append(isHttps(), other.isHttps())
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(getEndPointIP())
                .append(getDomainId())
                .append(getProject())
                .append(getUser())
                .append(getPassword())
                .append(isHttps())
                .toHashCode();
    }
}
