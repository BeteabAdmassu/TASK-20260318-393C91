package com.mindflow.security.workflow;

public record WorkflowProgressStep(
        String label,
        boolean completed,
        boolean active
) {
}
