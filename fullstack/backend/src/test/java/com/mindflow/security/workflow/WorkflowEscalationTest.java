package com.mindflow.security.workflow;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class WorkflowEscalationTest {

    @Autowired
    private WorkflowTaskRepository repository;

    @Autowired
    private WorkflowService workflowService;

    @Test
    void evaluateEscalationsMarksOnlyStaleActionableTasks() {
        WorkflowTaskEntity staleSubmitted = saveTask(WorkflowStatus.SUBMITTED, Instant.now().minusSeconds(25 * 3600L));
        WorkflowTaskEntity staleInReview = saveTask(WorkflowStatus.IN_REVIEW, Instant.now().minusSeconds(26 * 3600L));
        WorkflowTaskEntity freshSubmitted = saveTask(WorkflowStatus.SUBMITTED, Instant.now().minusSeconds(2 * 3600L));
        WorkflowTaskEntity staleApproved = saveTask(WorkflowStatus.APPROVED, Instant.now().minusSeconds(30 * 3600L));

        List<WorkflowTaskResponse> escalated = workflowService.evaluateEscalations();

        assertThat(escalated)
                .extracting(WorkflowTaskResponse::id)
                .contains(staleSubmitted.getId(), staleInReview.getId())
                .doesNotContain(freshSubmitted.getId(), staleApproved.getId());

        assertThat(repository.findById(staleSubmitted.getId())).get().extracting(WorkflowTaskEntity::isEscalated).isEqualTo(true);
        assertThat(repository.findById(staleInReview.getId())).get().extracting(WorkflowTaskEntity::isEscalated).isEqualTo(true);
        assertThat(repository.findById(freshSubmitted.getId())).get().extracting(WorkflowTaskEntity::isEscalated).isEqualTo(false);
        assertThat(repository.findById(staleApproved.getId())).get().extracting(WorkflowTaskEntity::isEscalated).isEqualTo(false);
    }

    @Test
    void escalationSchedulerIsConfiguredWithScheduledAnnotation() throws Exception {
        Scheduled scheduled = WorkflowEscalationScheduler.class
                .getDeclaredMethod("evaluateStaleTasks")
                .getAnnotation(Scheduled.class);

        assertThat(scheduled).isNotNull();
        assertThat(scheduled.fixedDelayString()).isEqualTo("${app.workflow.escalation-check-ms:60000}");
    }

    private WorkflowTaskEntity saveTask(WorkflowStatus status, Instant lastActionAt) {
        WorkflowTaskEntity task = new WorkflowTaskEntity();
        task.setType(WorkflowType.ROUTE_DATA_CHANGE);
        task.setMode(WorkflowMode.CONDITIONAL);
        task.setStatus(status);
        task.setTitle("Escalation test");
        task.setPayload("{}");
        task.setSubmittedBy("dispatcher_a");
        task.setAssignedTo("dispatcher_a");
        task.setCollaborators(Set.of("dispatcher_a"));
        task.setCurrentStep(1);
        task.setTotalSteps(3);
        task.setRequiredApprovals(1);
        task.setReceivedApprovals(0);
        task.setEscalated(false);
        WorkflowTaskEntity saved = repository.save(task);
        saved.setLastActionAt(lastActionAt);
        return repository.save(saved);
    }
}
