package org.osc.core.broker.service.exceptions;

import com.mcafee.vmidc.server.Server;

public class VmidcException extends Exception {
    private static final long serialVersionUID = 1L;

    public VmidcException(String s) {
        super(Server.PRODUCT_NAME + ": " + s);
    }

    public VmidcException(Throwable e) {
        super(e);
    }

}
