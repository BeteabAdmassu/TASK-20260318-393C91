package com.mindflow.security.workflow;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "workflow_tasks")
public class WorkflowTaskEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 40)
    private WorkflowType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode", nullable = false, length = 20)
    private WorkflowMode mode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private WorkflowStatus status;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "payload", nullable = false, length = 3000)
    private String payload;

    @Column(name = "submitted_by", nullable = false, length = 120)
    private String submittedBy;

    @Column(name = "assigned_to", nullable = false, length = 120)
    private String assignedTo;

    @Column(name = "collaborators", nullable = false, length = 1000)
    private String collaborators = "";

    @Column(name = "current_step", nullable = false)
    private int currentStep;

    @Column(name = "total_steps", nullable = false)
    private int totalSteps;

    @Column(name = "required_approvals", nullable = false)
    private int requiredApprovals;

    @Column(name = "received_approvals", nullable = false)
    private int receivedApprovals;

    @Column(name = "escalated", nullable = false)
    private boolean escalated;

    @Column(name = "last_action_at", nullable = false)
    private Instant lastActionAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        this.lastActionAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public WorkflowType getType() {
        return type;
    }

    public void setType(WorkflowType type) {
        this.type = type;
    }

    public WorkflowMode getMode() {
        return mode;
    }

    public void setMode(WorkflowMode mode) {
        this.mode = mode;
    }

    public WorkflowStatus getStatus() {
        return status;
    }

    public void setStatus(WorkflowStatus status) {
        this.status = status;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public String getSubmittedBy() {
        return submittedBy;
    }

    public void setSubmittedBy(String submittedBy) {
        this.submittedBy = submittedBy;
    }

    public String getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(String assignedTo) {
        this.assignedTo = assignedTo;
    }

    public String getCollaborators() {
        return collaborators;
    }

    public void setCollaborators(String collaborators) {
        this.collaborators = collaborators;
    }

    public Set<String> collaboratorsAsSet() {
        Set<String> set = new HashSet<>();
        if (collaborators == null || collaborators.isBlank()) {
            return set;
        }
        for (String part : collaborators.split(",")) {
            if (!part.isBlank()) {
                set.add(part.trim());
            }
        }
        return set;
    }

    public void setCollaboratorsFromSet(Set<String> values) {
        this.collaborators = String.join(",", values);
    }

    public int getCurrentStep() {
        return currentStep;
    }

    public void setCurrentStep(int currentStep) {
        this.currentStep = currentStep;
    }

    public int getTotalSteps() {
        return totalSteps;
    }

    public void setTotalSteps(int totalSteps) {
        this.totalSteps = totalSteps;
    }

    public int getRequiredApprovals() {
        return requiredApprovals;
    }

    public void setRequiredApprovals(int requiredApprovals) {
        this.requiredApprovals = requiredApprovals;
    }

    public int getReceivedApprovals() {
        return receivedApprovals;
    }

    public void setReceivedApprovals(int receivedApprovals) {
        this.receivedApprovals = receivedApprovals;
    }

    public boolean isEscalated() {
        return escalated;
    }

    public void setEscalated(boolean escalated) {
        this.escalated = escalated;
    }

    public Instant getLastActionAt() {
        return lastActionAt;
    }

    public void setLastActionAt(Instant lastActionAt) {
        this.lastActionAt = lastActionAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
