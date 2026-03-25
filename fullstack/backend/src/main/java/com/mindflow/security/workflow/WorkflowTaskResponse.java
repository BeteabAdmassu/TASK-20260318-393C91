package com.mindflow.security.workflow;

import java.time.Instant;

public record WorkflowTaskResponse(
        Long id,
        WorkflowType type,
        WorkflowMode mode,
        WorkflowStatus status,
        String title,
        String payload,
        String submittedBy,
        String assignedTo,
        int currentStep,
        int totalSteps,
        int requiredApprovals,
        int receivedApprovals,
        boolean escalated,
        boolean timeoutWarning,
        Instant lastActionAt,
        Instant createdAt
) {
}
