package com.joyjit.scms.core.exception;

/**
 * Custom exception indicating that the calling user does not have sufficient permissions
 * to perform the requested operation.
 */
public class PermissionDeniedException extends RuntimeException {
    public PermissionDeniedException(String message) {
        super(message);
    }
}