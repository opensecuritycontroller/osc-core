package org.osc.core.rest.client.exception;

@SuppressWarnings("serial")
public class CorruptedPidException extends Exception {
    public CorruptedPidException() {
        super();
    }

    public CorruptedPidException(NumberFormatException e) {
        super(e);
    }
}
