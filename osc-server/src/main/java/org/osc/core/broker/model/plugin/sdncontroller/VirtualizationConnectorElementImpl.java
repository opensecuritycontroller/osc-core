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
package org.osc.core.broker.model.plugin.sdncontroller;

import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.broker.util.crypto.SslContextProvider;
import org.osc.core.broker.util.crypto.X509TrustManagerFactory;
import org.osc.sdk.controller.element.VirtualizationConnectorElement;

public class VirtualizationConnectorElementImpl implements VirtualizationConnectorElement {

    private final VirtualizationConnector virtualizationConnector;

    public VirtualizationConnectorElementImpl(VirtualizationConnector shallowClone) {
        this.virtualizationConnector = shallowClone;
    }

    @Override
    public String getName() {
        return this.virtualizationConnector.getName();
    }

    @Override
    public String getControllerIpAddress() {
        return this.virtualizationConnector.getControllerIpAddress();
    }

    @Override
    public String getControllerUsername() {
        return this.virtualizationConnector.getControllerUsername();
    }

    @Override
    public String getControllerPassword() {
        return this.virtualizationConnector.getControllerPassword();
    }

    @Override
    public boolean isControllerHttps() {
        // TODO: emanoel - Future. Need to add support for Controller HTTPS access
        return this.virtualizationConnector.getControllerType().equals("NSC") ? false : true;
    }

    @Override
    public String getProviderIpAddress() {
        return this.virtualizationConnector.getProviderIpAddress();
    }

    @Override
    public String getProviderUsername() {
        return this.virtualizationConnector.getProviderUsername();
    }

    @Override
    public String getProviderPassword() {
        return this.virtualizationConnector.getProviderPassword();
    }

    @Override
    public String getProviderAdminTenantName() {
        return this.virtualizationConnector.getProviderAdminTenantName();
    }

    @Override
    public String getProviderAdminDomainId() {
        return this.virtualizationConnector.getAdminDomainId();
    }

    @Override
    public boolean isProviderHttps() {
        return this.virtualizationConnector.isProviderHttps();
    }

    @Override
    public Map<String, String> getProviderAttributes() {
        return this.virtualizationConnector.getProviderAttributes();
    }

    @Override
    public SSLContext getSslContext() {
        return SslContextProvider.getInstance().getSSLContext();
    }

    @Override
    public TrustManager[] getTruststoreManager() throws Exception {
        return new TrustManager[]{X509TrustManagerFactory.getInstance()};
    }

}
