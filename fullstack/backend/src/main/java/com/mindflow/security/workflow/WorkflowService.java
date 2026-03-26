package com.mindflow.security.workflow;

import com.mindflow.security.common.OwnershipDeniedException;
import com.mindflow.security.common.ConcurrencyConflictException;
import com.mindflow.security.common.ResourceNotFoundException;
import com.mindflow.security.common.TenantContext;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class WorkflowService {

    private static final Duration ESCALATION_TIMEOUT = Duration.ofHours(24);

    private final WorkflowTaskRepository repository;
    private final WorkflowStateMachineService stateMachineService;

    public WorkflowService(WorkflowTaskRepository repository,
                           WorkflowStateMachineService stateMachineService) {
        this.repository = repository;
        this.stateMachineService = stateMachineService;
    }

    @Transactional
    public WorkflowTaskResponse createTask(CreateWorkflowTaskRequest request, String submittedBy) {
        String tenantId = TenantContext.getTenantId();
        WorkflowTaskEntity task = new WorkflowTaskEntity();
        task.setType(request.type());
        task.setMode(request.mode());
        task.setStatus(WorkflowStatus.SUBMITTED);
        task.setTitle(request.title());
        task.setPayload(request.payload());
        task.setSubmittedBy(submittedBy);
        task.setAssignedTo(submittedBy);
        Set<String> collaborators = task.collaboratorsAsSet();
        collaborators.add(submittedBy);
        if (request.collaborators() != null) {
            collaborators.addAll(request.collaborators());
        }
        task.setCollaboratorsFromSet(collaborators);
        task.setCurrentStep(1);
        task.setTotalSteps(3);
        int required = request.requiredApprovals() == null ? defaultApprovals(request.mode()) : Math.max(1, request.requiredApprovals());
        task.setRequiredApprovals(required);
        task.setReceivedApprovals(0);
        task.setEscalated(false);
        task.setLastActionAt(Instant.now());
        task.setTenantId(tenantId);
        return toResponse(repository.save(task));
    }

    @Transactional(readOnly = true)
    public List<WorkflowTaskResponse> listTasks(String actor) {
        String tenantId = TenantContext.getTenantId();
        Map<Long, WorkflowTaskResponse> merged = new LinkedHashMap<>();
        for (WorkflowTaskEntity task : repository.findByAssignedToAndTenantIdOrderByCreatedAtDesc(actor, tenantId)) {
            merged.put(task.getId(), toResponse(task));
        }
        for (WorkflowTaskEntity task : repository.findByCollaboratorsContainsAndTenantIdOrderByCreatedAtDesc(actor, tenantId)) {
            if (canAccess(task, actor)) {
                merged.put(task.getId(), toResponse(task));
            }
        }
        return merged.values().stream().toList();
    }

    @Transactional(readOnly = true)
    public WorkflowDetailsResponse getTask(Long id, String actor) {
        WorkflowTaskEntity task = fetch(id, actor, TenantContext.getTenantId());
        return new WorkflowDetailsResponse(toResponse(task), buildProgress(task));
    }

    @Transactional
    public WorkflowTaskResponse approve(Long id, String actor) {
        return withOptimisticLockGuard(() -> {
            WorkflowTaskEntity task = fetch(id, actor, TenantContext.getTenantId());
            stateMachineService.approve(task);
            task.setLastActionAt(Instant.now());
            task.setEscalated(false);
            return toResponse(repository.save(task));
        });
    }

    @Transactional
    public WorkflowTaskResponse reject(Long id, String actor) {
        return withOptimisticLockGuard(() -> {
            WorkflowTaskEntity task = fetch(id, actor, TenantContext.getTenantId());
            stateMachineService.reject(task);
            task.setLastActionAt(Instant.now());
            task.setEscalated(false);
            return toResponse(repository.save(task));
        });
    }

    @Transactional
    public WorkflowTaskResponse returnToSubmitter(Long id, String actor) {
        return withOptimisticLockGuard(() -> {
            WorkflowTaskEntity task = fetch(id, actor, TenantContext.getTenantId());
            stateMachineService.returnToSubmitter(task);
            task.setLastActionAt(Instant.now());
            task.setEscalated(false);
            return toResponse(repository.save(task));
        });
    }

    @Transactional
    public List<WorkflowTaskResponse> batchApprove(List<Long> ids, String actor) {
        return ids.stream().map(id -> approve(id, actor)).toList();
    }

    private WorkflowTaskResponse withOptimisticLockGuard(java.util.concurrent.Callable<WorkflowTaskResponse> action) {
        try {
            return action.call();
        } catch (ObjectOptimisticLockingFailureException ex) {
            throw new ConcurrencyConflictException("Workflow task was modified concurrently. Please refresh and retry.");
        } catch (ConcurrencyConflictException ex) {
            throw ex;
        } catch (Exception ex) {
            if (ex instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException(ex);
        }
    }

    @Transactional
    public List<WorkflowTaskResponse> evaluateEscalations() {
        String tenantId = TenantContext.getTenantId();
        Instant cutoff = Instant.now().minus(ESCALATION_TIMEOUT);
        List<WorkflowTaskEntity> staleTasks = repository.findByStatusInAndLastActionAtBeforeAndTenantId(
                List.of(WorkflowStatus.SUBMITTED, WorkflowStatus.IN_REVIEW),
                cutoff,
                tenantId
        );

        for (WorkflowTaskEntity task : staleTasks) {
            task.setEscalated(true);
        }
        repository.saveAll(staleTasks);
        return staleTasks.stream().map(this::toResponse).toList();
    }

    private WorkflowTaskEntity fetch(Long id, String actor, String tenantId) {
        WorkflowTaskEntity task = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        if (!tenantId.equals(task.getTenantId())) {
            throw new OwnershipDeniedException("Workflow task access denied for this tenant");
        }
        if (!canAccess(task, actor)) {
            throw new OwnershipDeniedException("Workflow task access denied for this user");
        }
        return task;
    }

    private boolean canAccess(WorkflowTaskEntity task, String actor) {
        if (actor.equals(task.getAssignedTo())) {
            return true;
        }
        if (task.getMode() == WorkflowMode.CONDITIONAL) {
            return false;
        }
        return task.collaboratorsAsSet().contains(actor);
    }

    private int defaultApprovals(WorkflowMode mode) {
        return switch (mode) {
            case JOINT, PARALLEL -> 2;
            case CONDITIONAL -> 1;
        };
    }

    private List<WorkflowProgressStep> buildProgress(WorkflowTaskEntity task) {
        return List.of(
                new WorkflowProgressStep("Submitted", task.getCurrentStep() >= 1, task.getCurrentStep() == 1),
                new WorkflowProgressStep("In Review", task.getCurrentStep() >= 2, task.getCurrentStep() == 2),
                new WorkflowProgressStep("Final Decision", task.getCurrentStep() >= 3 || isFinal(task), task.getCurrentStep() == 3 || isFinal(task))
        );
    }

    private boolean isFinal(WorkflowTaskEntity task) {
        return task.getStatus() == WorkflowStatus.APPROVED ||
                task.getStatus() == WorkflowStatus.REJECTED ||
                task.getStatus() == WorkflowStatus.RETURNED;
    }

    private WorkflowTaskResponse toResponse(WorkflowTaskEntity task) {
        boolean timeout = Duration.between(task.getLastActionAt(), Instant.now()).toHours() >= 24
                && (task.getStatus() == WorkflowStatus.SUBMITTED || task.getStatus() == WorkflowStatus.IN_REVIEW);
        return new WorkflowTaskResponse(
                task.getId(),
                task.getType(),
                task.getMode(),
                task.getStatus(),
                task.getTitle(),
                task.getPayload(),
                task.getSubmittedBy(),
                task.getAssignedTo(),
                task.getCurrentStep(),
                task.getTotalSteps(),
                task.getRequiredApprovals(),
                task.getReceivedApprovals(),
                task.isEscalated(),
                timeout,
                task.getLastActionAt(),
                task.getCreatedAt()
        );
    }
}
