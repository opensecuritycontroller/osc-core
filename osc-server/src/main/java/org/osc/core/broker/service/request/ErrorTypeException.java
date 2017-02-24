package org.osc.core.broker.service.request;

import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;

/**
 * Wraps an exception and provides more context on the type of exception
 */
@SuppressWarnings("serial")
public class ErrorTypeException extends VmidcBrokerValidationException {

    public enum ErrorType {
        CONTROLLER_EXCEPTION, PROVIDER_EXCEPTION, IP_CHANGED_EXCEPTION, MANAGER_CONNECTOR_EXCEPTION, RABBITMQ_EXCEPTION;
    }

    private ErrorType type;

    public ErrorTypeException(Throwable e, ErrorType type) {
        super(e);
        this.type = type;
    }

    public ErrorTypeException(String s, ErrorType type) {
        super(s);
        this.type = type;
    }

    public ErrorType getType() {
        return this.type;
    }

}
