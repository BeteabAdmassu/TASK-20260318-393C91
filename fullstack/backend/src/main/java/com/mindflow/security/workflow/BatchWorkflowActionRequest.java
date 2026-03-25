package com.mindflow.security.workflow;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record BatchWorkflowActionRequest(
        @NotEmpty List<@Positive Long> taskIds,
        String reason
) {
}
