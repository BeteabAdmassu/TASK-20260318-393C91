package com.mindflow.security.integration;

import java.time.Instant;

public record ImportJobResponse(
        Long jobId,
        ImportStatus status,
        String sourceName,
        int totalRows,
        int successRows,
        int failedRows,
        Instant createdAt,
        Instant updatedAt
) {
}
