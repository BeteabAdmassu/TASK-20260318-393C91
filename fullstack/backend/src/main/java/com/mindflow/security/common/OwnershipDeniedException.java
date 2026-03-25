package com.mindflow.security.common;

public class OwnershipDeniedException extends RuntimeException {
    public OwnershipDeniedException(String message) {
        super(message);
    }
}
