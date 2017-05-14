package org.beryx.textio.web;

public class DataApiProviderException extends RuntimeException {
    public DataApiProviderException(String message) {
        super(message);
    }

    public DataApiProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
