package org.osc.core.broker.service.exceptions;

public class ActionNotSupportedException extends VmidcBrokerInvalidRequestException {

    private static final long serialVersionUID = 1L;

    public ActionNotSupportedException(String s){
        super(s);
    }

}
