package com.mindflow.security.workflow;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface WorkflowTaskRepository extends JpaRepository<WorkflowTaskEntity, Long> {
    List<WorkflowTaskEntity> findByStatusInAndTenantId(List<WorkflowStatus> statuses, String tenantId);

    List<WorkflowTaskEntity> findByAssignedToAndTenantIdOrderByCreatedAtDesc(String assignedTo, String tenantId);

    List<WorkflowTaskEntity> findByCollaboratorsContainsAndTenantIdOrderByCreatedAtDesc(String collaborator, String tenantId);

    List<WorkflowTaskEntity> findByStatusInAndLastActionAtBeforeAndTenantId(List<WorkflowStatus> statuses, Instant cutoff, String tenantId);
}
