package com.mindflow.security.workflow;

import jakarta.validation.constraints.NotBlank;

public record WorkflowActionRequest(
        @NotBlank String reason
) {
}
