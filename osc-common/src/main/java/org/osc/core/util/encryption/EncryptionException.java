package org.osc.core.util.encryption;

public class EncryptionException extends Exception {
    private static final long serialVersionUID = -52733376278542276L;

    EncryptionException(String message, Throwable cause) {
        super(message, cause);
    }

    public EncryptionException(String message) {
        super(message);
    }
}