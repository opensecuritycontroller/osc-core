package org.osc.core.broker.service.exceptions;

public class VmidcBrokerValidationException extends VmidcException {

    private static final long serialVersionUID = 1L;

    public VmidcBrokerValidationException(String s) {
        super(s);
    }

    public VmidcBrokerValidationException(Throwable e) {
        super(e);
    }
}
