package com.mindflow.security.workflow;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface WorkflowTaskRepository extends JpaRepository<WorkflowTaskEntity, Long> {
    List<WorkflowTaskEntity> findByStatusIn(List<WorkflowStatus> statuses);

    List<WorkflowTaskEntity> findByAssignedToOrderByCreatedAtDesc(String assignedTo);

    List<WorkflowTaskEntity> findByCollaboratorsContainsOrderByCreatedAtDesc(String collaborator);

    List<WorkflowTaskEntity> findByStatusInAndLastActionAtBefore(List<WorkflowStatus> statuses, Instant cutoff);
}
