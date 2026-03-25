package com.mindflow.security.integration;

public record StopVersionResponse(
        String stopName,
        String fieldName,
        String oldValue,
        String newValue,
        int versionNumber,
        Long importJobId
) {
}
