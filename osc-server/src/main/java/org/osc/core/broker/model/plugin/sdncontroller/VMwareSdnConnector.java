package org.osc.core.broker.model.plugin.sdncontroller;

import org.apache.commons.lang.StringUtils;
import org.osc.core.broker.model.entities.virtualization.VirtualizationConnector;
import org.osc.core.util.EncryptionUtil;
import org.osc.sdk.sdn.element.ConnectorElement;

import javax.net.ssl.SSLContext;

public class VMwareSdnConnector implements ConnectorElement {
	private String ipAddress;
	private String userName;
	private String password;
	private SSLContext sslContext;

    public VMwareSdnConnector(VirtualizationConnector vc) {
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
        this.password = EncryptionUtil.decrypt(vc.getControllerPassword());
        this.sslContext = vc.getSslContext();
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
