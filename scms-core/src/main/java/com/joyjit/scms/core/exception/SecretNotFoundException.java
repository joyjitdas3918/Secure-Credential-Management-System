package com.joyjit.scms.core.exception;

/**
 * Custom exception indicating that a requested secret could not be found.
 */
public class SecretNotFoundException extends RuntimeException {
    public SecretNotFoundException(String message) {
        super(message);
    }
    public SecretNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}