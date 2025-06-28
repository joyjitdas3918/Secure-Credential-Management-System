package com.joyjit.scms.core.exception;

/**
 * Custom exception for errors occurring during encryption or decryption operations.
 */
public class EncryptionException extends RuntimeException {
    public EncryptionException(String message) {
        super(message);
    }
    public EncryptionException(String message, Throwable cause) {
        super(message, cause);
    }
}