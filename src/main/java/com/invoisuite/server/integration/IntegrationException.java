package com.invoisuite.server.integration;

/** A failure talking to an external platform, with a message that is safe to show to the user. */
public class IntegrationException extends RuntimeException {
    public IntegrationException(String message) {
        super(message);
    }

    public IntegrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
