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

import javax.net.ssl.SSLContext;

import org.apache.commons.lang.StringUtils;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.rest.client.crypto.SslContextProvider;
import org.osc.core.util.EncryptionUtil;
import org.osc.core.util.encryption.EncryptionException;
import org.osc.sdk.sdn.element.ConnectorElement;

public class VMwareSdnConnector implements ConnectorElement {
	private String ipAddress;
	private String userName;
	private String password;
	private SSLContext sslContext;

    public VMwareSdnConnector(VirtualizationConnector vc) throws EncryptionException {
        if (vc == null) {
            throw new IllegalArgumentException("The provided virtualization connector cannot be null.");
        }

        if (StringUtils.isEmpty(vc.getControllerIpAddress()) ||
                StringUtils.isEmpty(vc.getControllerUsername()) ||
                StringUtils.isEmpty(vc.getControllerPassword())) {
            throw new IllegalArgumentException("The provided virtualization connector is missing the controller ip address, user name or password.");
        }

        this.ipAddress = vc.getControllerIpAddress();
        this.userName = vc.getControllerUsername();
        this.password = EncryptionUtil.decryptAESCTR(vc.getControllerPassword());
        this.sslContext = SslContextProvider.getInstance().getSSLContext();
    }

    /**
     * @see org.osc.sdk.sdn.element.ConnectorElement#getIpAddress()
     */
    @Override
    public String getIpAddress() {
        return this.ipAddress;
    }

    /**
     * @see org.osc.sdk.sdn.element.ConnectorElement#getUserName()
     */
    @Override
    public String getUserName() {
        return this.userName;
    }

	/**
	 * @see ConnectorElement#getSslContext()
	 */
	@Override
	public SSLContext getSslContext() {
		return this.sslContext;
	}
    /**
     * @see org.osc.sdk.sdn.element.ConnectorElement#getPassword()
     */
    @Override
    public String getPassword() {
        return this.password;
    }
}
