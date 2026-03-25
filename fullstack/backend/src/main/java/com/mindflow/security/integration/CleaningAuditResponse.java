package com.mindflow.security.integration;

public record CleaningAuditResponse(
        String sourceRef,
        String fieldName,
        String rawValue,
        String cleanedValue,
        String action
) {
}
