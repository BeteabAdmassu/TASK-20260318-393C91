package com.mindflow.security.workflow;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateWorkflowTaskRequest(
        @NotNull WorkflowType type,
        @NotNull WorkflowMode mode,
        @NotBlank String title,
        @NotBlank String payload,
        Integer requiredApprovals,
        List<String> collaborators
) {
}
