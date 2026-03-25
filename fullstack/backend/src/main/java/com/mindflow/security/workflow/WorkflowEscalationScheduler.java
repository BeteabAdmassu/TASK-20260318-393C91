package com.mindflow.security.workflow;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class WorkflowEscalationScheduler {

    private final WorkflowService workflowService;

    public WorkflowEscalationScheduler(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @Scheduled(fixedDelayString = "${app.workflow.escalation-check-ms:60000}")
    public void evaluateStaleTasks() {
        workflowService.evaluateEscalations();
    }
}
