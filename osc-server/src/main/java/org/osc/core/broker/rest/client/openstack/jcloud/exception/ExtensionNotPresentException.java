package org.osc.core.broker.rest.client.openstack.jcloud.exception;

import org.osc.core.broker.view.common.VmidcMessages;
import org.osc.core.broker.view.common.VmidcMessages_;

@SuppressWarnings("serial")
public class ExtensionNotPresentException extends RuntimeException {

    public ExtensionNotPresentException(String extensionName) {
        super(VmidcMessages.getString(VmidcMessages_.OS_EXTENSION_NOT_PRESENT, extensionName));
    }

}
