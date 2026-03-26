package com.mindflow.security.workflow;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dispatcher/workflows")
@Validated
public class WorkflowController {

    private final WorkflowService workflowService;

    public WorkflowController(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('DISPATCHER', 'ADMIN')")
    public ResponseEntity<WorkflowTaskResponse> createTask(
            @Valid @RequestBody CreateWorkflowTaskRequest request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(workflowService.createTask(request, authentication.getName()));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('DISPATCHER', 'ADMIN')")
    public ResponseEntity<List<WorkflowTaskResponse>> listTasks(Authentication authentication) {
        return ResponseEntity.ok(workflowService.listTasks(authentication.getName()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('DISPATCHER', 'ADMIN')")
    public ResponseEntity<WorkflowDetailsResponse> getTask(@PathVariable @Positive Long id,
                                                           Authentication authentication) {
        return ResponseEntity.ok(workflowService.getTask(id, authentication.getName()));
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('DISPATCHER', 'ADMIN')")
    public ResponseEntity<WorkflowTaskResponse> approve(@PathVariable @Positive Long id,
                                                         @Valid @RequestBody WorkflowActionRequest request,
                                                         Authentication authentication) {
        return ResponseEntity.ok(workflowService.approve(id, authentication.getName()));
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('DISPATCHER', 'ADMIN')")
    public ResponseEntity<WorkflowTaskResponse> reject(@PathVariable @Positive Long id,
                                                        @Valid @RequestBody WorkflowActionRequest request,
                                                        Authentication authentication) {
        return ResponseEntity.ok(workflowService.reject(id, authentication.getName()));
    }

    @PutMapping("/{id}/return")
    @PreAuthorize("hasAnyRole('DISPATCHER', 'ADMIN')")
    public ResponseEntity<WorkflowTaskResponse> returnTask(@PathVariable @Positive Long id,
                                                            @Valid @RequestBody WorkflowActionRequest request,
                                                            Authentication authentication) {
        return ResponseEntity.ok(workflowService.returnToSubmitter(id, authentication.getName()));
    }

    @PutMapping("/batch/approve")
    @PreAuthorize("hasAnyRole('DISPATCHER', 'ADMIN')")
    public ResponseEntity<List<WorkflowTaskResponse>> batchApprove(
            @Valid @RequestBody BatchWorkflowActionRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(workflowService.batchApprove(request.taskIds(), authentication.getName()));
    }

    @PostMapping("/escalations/evaluate")
    @PreAuthorize("hasAnyRole('DISPATCHER', 'ADMIN')")
    public ResponseEntity<List<WorkflowTaskResponse>> evaluateEscalations() {
        return ResponseEntity.ok(workflowService.evaluateEscalations());
    }
}
