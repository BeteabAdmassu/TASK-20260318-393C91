package com.mindflow.security.workflow;

import org.springframework.stereotype.Service;

@Service
public class WorkflowStateMachineService {

    public WorkflowTaskEntity approve(WorkflowTaskEntity task) {
        ensureActionable(task);
        switch (task.getMode()) {
            case JOINT -> approveJoint(task);
            case PARALLEL -> approveParallel(task);
            case CONDITIONAL -> approveConditional(task);
        }
        return task;
    }

    public WorkflowTaskEntity reject(WorkflowTaskEntity task) {
        ensureActionable(task);
        task.setStatus(WorkflowStatus.REJECTED);
        return task;
    }

    public WorkflowTaskEntity returnToSubmitter(WorkflowTaskEntity task) {
        ensureActionable(task);
        task.setStatus(WorkflowStatus.RETURNED);
        task.setCurrentStep(1);
        task.setReceivedApprovals(0);
        return task;
    }

    private void ensureActionable(WorkflowTaskEntity task) {
        if (task.getStatus() == WorkflowStatus.APPROVED ||
                task.getStatus() == WorkflowStatus.REJECTED ||
                task.getStatus() == WorkflowStatus.RETURNED) {
            throw new IllegalArgumentException("Task is already finalized");
        }
    }

    private void approveJoint(WorkflowTaskEntity task) {
        task.setReceivedApprovals(task.getReceivedApprovals() + 1);
        task.setStatus(WorkflowStatus.IN_REVIEW);
        task.setCurrentStep(2);
        if (task.getReceivedApprovals() >= task.getRequiredApprovals()) {
            task.setStatus(WorkflowStatus.APPROVED);
            task.setCurrentStep(task.getTotalSteps());
        }
    }

    private void approveParallel(WorkflowTaskEntity task) {
        task.setReceivedApprovals(task.getReceivedApprovals() + 1);
        if (task.getReceivedApprovals() >= task.getRequiredApprovals()) {
            task.setStatus(WorkflowStatus.APPROVED);
            task.setCurrentStep(task.getTotalSteps());
        } else {
            task.setStatus(WorkflowStatus.IN_REVIEW);
            task.setCurrentStep(Math.min(task.getTotalSteps(), task.getCurrentStep() + 1));
        }
    }

    private void approveConditional(WorkflowTaskEntity task) {
        task.setStatus(WorkflowStatus.IN_REVIEW);
        if (task.getCurrentStep() == 1 && task.getPayload() != null && task.getPayload().toLowerCase().contains("urgent")) {
            task.setCurrentStep(Math.min(task.getTotalSteps(), task.getCurrentStep() + 2));
        } else {
            task.setCurrentStep(Math.min(task.getTotalSteps(), task.getCurrentStep() + 1));
        }
        if (task.getCurrentStep() >= task.getTotalSteps()) {
            task.setStatus(WorkflowStatus.APPROVED);
        }
    }
}
