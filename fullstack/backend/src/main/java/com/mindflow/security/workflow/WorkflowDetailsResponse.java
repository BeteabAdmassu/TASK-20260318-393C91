package com.mindflow.security.workflow;

import java.util.List;

public record WorkflowDetailsResponse(
        WorkflowTaskResponse task,
        List<WorkflowProgressStep> progress
) {
}
