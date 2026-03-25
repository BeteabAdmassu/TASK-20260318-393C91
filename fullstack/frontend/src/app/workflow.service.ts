import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  WorkflowDetails,
  WorkflowMode,
  WorkflowTask,
  WorkflowType
} from './auth.models';

export interface CreateWorkflowTaskRequest {
  type: WorkflowType;
  mode: WorkflowMode;
  title: string;
  payload: string;
  requiredApprovals?: number;
}

@Injectable({ providedIn: 'root' })
export class WorkflowService {
  constructor(private readonly http: HttpClient) {}

  listTasks(): Observable<WorkflowTask[]> {
    return this.http.get<WorkflowTask[]>('/api/dispatcher/workflows');
  }

  getTask(id: number): Observable<WorkflowDetails> {
    return this.http.get<WorkflowDetails>(`/api/dispatcher/workflows/${id}`);
  }

  createTask(payload: CreateWorkflowTaskRequest): Observable<WorkflowTask> {
    return this.http.post<WorkflowTask>('/api/dispatcher/workflows', payload);
  }

  approve(id: number, reason: string): Observable<WorkflowTask> {
    return this.http.put<WorkflowTask>(`/api/dispatcher/workflows/${id}/approve`, { reason });
  }

  reject(id: number, reason: string): Observable<WorkflowTask> {
    return this.http.put<WorkflowTask>(`/api/dispatcher/workflows/${id}/reject`, { reason });
  }

  returnToSubmitter(id: number, reason: string): Observable<WorkflowTask> {
    return this.http.put<WorkflowTask>(`/api/dispatcher/workflows/${id}/return`, { reason });
  }

  batchApprove(taskIds: number[], reason: string): Observable<WorkflowTask[]> {
    return this.http.put<WorkflowTask[]>('/api/dispatcher/workflows/batch/approve', {
      taskIds,
      reason
    });
  }

  evaluateEscalations(): Observable<WorkflowTask[]> {
    return this.http.post<WorkflowTask[]>('/api/dispatcher/workflows/escalations/evaluate', {});
  }
}
