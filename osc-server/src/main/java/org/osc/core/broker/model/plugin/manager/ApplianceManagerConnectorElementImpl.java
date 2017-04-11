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
package org.osc.core.broker.model.plugin.manager;

import java.util.Arrays;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import org.osc.core.broker.model.entities.management.ApplianceManagerConnector;
import org.osc.core.rest.client.crypto.SslContextProvider;
import org.osc.core.rest.client.crypto.X509TrustManagerFactory;
import org.osc.sdk.manager.element.ApplianceManagerConnectorElement;
import org.osc.sdk.manager.element.ManagerTypeElement;

public class ApplianceManagerConnectorElementImpl implements ApplianceManagerConnectorElement {

    private final ApplianceManagerConnector applianceManagerConnector;

    public ApplianceManagerConnectorElementImpl(ApplianceManagerConnector applianceManagerConnector) {
        this.applianceManagerConnector = applianceManagerConnector;
    }

    @Override
    public String getName() {
        return this.applianceManagerConnector.getName();
    }

    @Override
    public String getIpAddress() {
        return this.applianceManagerConnector.getIpAddress();
    }

    @Override
    public String getUsername() {
        return this.applianceManagerConnector.getUsername();
    }

    @Override
    public String getPassword() {
        return this.applianceManagerConnector.getPassword();
    }

    @Override
    public String getApiKey() {
        return this.applianceManagerConnector.getApiKey();
    }

    @Override
    public ManagerTypeElement getManagerType() {
        return ManagerType.fromText(this.applianceManagerConnector.getManagerType());
    }

    @Override
    public String getLastKnownNotificationIpAddress() {
        return this.applianceManagerConnector.getLastKnownNotificationIpAddress();
    }

    @Override
    public byte[] getPublicKey() {
        byte[] publicKey = this.applianceManagerConnector.getPublicKey();
        return Arrays.copyOf(publicKey, publicKey.length);
    }

    @Override
    public String getClientIpAddress() {
        return this.applianceManagerConnector.getClientIpAddress();
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
