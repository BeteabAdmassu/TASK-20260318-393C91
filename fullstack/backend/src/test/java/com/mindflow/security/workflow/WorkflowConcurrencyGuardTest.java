package com.mindflow.security.workflow;

import com.mindflow.security.common.ConcurrencyConflictException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkflowConcurrencyGuardTest {

    @Mock
    private WorkflowTaskRepository repository;

    @Mock
    private WorkflowStateMachineService stateMachineService;

    @InjectMocks
    private WorkflowService workflowService;

    @Test
    void approveReturnsConflictWhenOptimisticLockFails() {
        WorkflowTaskEntity task = new WorkflowTaskEntity();
        task.setId(100L);
        task.setType(WorkflowType.ROUTE_DATA_CHANGE);
        task.setMode(WorkflowMode.JOINT);
        task.setStatus(WorkflowStatus.SUBMITTED);
        task.setTitle("Concurrent task");
        task.setPayload("{}");
        task.setSubmittedBy("dispatcher_a");
        task.setAssignedTo("dispatcher_a");
        task.setCollaborators(Set.of("dispatcher_a"));
        task.setCurrentStep(1);
        task.setTotalSteps(3);
        task.setRequiredApprovals(1);
        task.setReceivedApprovals(0);
        task.setEscalated(false);
        task.setLastActionAt(Instant.now());

        when(repository.findById(100L)).thenReturn(Optional.of(task));
        when(stateMachineService.approve(task)).thenReturn(task);
        when(repository.save(any(WorkflowTaskEntity.class)))
                .thenThrow(new ObjectOptimisticLockingFailureException(WorkflowTaskEntity.class, 100L));

        assertThatThrownBy(() -> workflowService.approve(100L, "dispatcher_a"))
                .isInstanceOf(ConcurrencyConflictException.class)
                .hasMessageContaining("modified concurrently");
    }
}
