import { Component, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgFor, NgIf } from '@angular/common';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatStepperModule } from '@angular/material/stepper';
import { WorkflowDetails, WorkflowTask } from './auth.models';
import { CreateWorkflowTaskRequest, WorkflowService } from './workflow.service';

@Component({
  selector: 'app-dispatcher-dashboard',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    NgIf,
    NgFor,
    MatTableModule,
    MatButtonModule,
    MatCheckboxModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatStepperModule
  ],
  template: `
    <main class="layout">
      <section class="card workflow-shell">
        <h2>Dispatcher Workflow & Approval Engine</h2>

        <mat-card>
          <mat-card-title>Create Task</mat-card-title>
          <mat-card-content>
            <form [formGroup]="createForm" class="create-grid" (ngSubmit)="createTask()">
              <mat-form-field appearance="outline">
                <mat-label>Workflow Type</mat-label>
                <mat-select formControlName="type">
                  <mat-option value="ROUTE_DATA_CHANGE">Route Data Change</mat-option>
                  <mat-option value="REMINDER_CONFIGURATION">Reminder Configuration</mat-option>
                  <mat-option value="ABNORMAL_DATA_REVIEW">Abnormal Data Review</mat-option>
                </mat-select>
              </mat-form-field>

              <mat-form-field appearance="outline">
                <mat-label>Mode</mat-label>
                <mat-select formControlName="mode">
                  <mat-option value="CONDITIONAL">Conditional</mat-option>
                  <mat-option value="JOINT">Joint Approval</mat-option>
                  <mat-option value="PARALLEL">Parallel Approval</mat-option>
                </mat-select>
              </mat-form-field>

              <mat-form-field appearance="outline">
                <mat-label>Title</mat-label>
                <input matInput formControlName="title" />
              </mat-form-field>

              <mat-form-field appearance="outline">
                <mat-label>Payload</mat-label>
                <input matInput formControlName="payload" />
              </mat-form-field>

              <mat-form-field appearance="outline">
                <mat-label>Required Approvals</mat-label>
                <input matInput type="number" formControlName="requiredApprovals" />
              </mat-form-field>

              <button mat-raised-button color="primary" type="submit" [disabled]="createForm.invalid">
                Submit Task
              </button>
            </form>
          </mat-card-content>
        </mat-card>

        <mat-card>
          <mat-card-title>Task Queue</mat-card-title>
          <mat-card-content>
            <div class="toolbar">
              <button mat-stroked-button color="primary" (click)="refresh()">Refresh</button>
              <button mat-stroked-button color="accent" (click)="evaluateEscalations()">Evaluate Escalations</button>
              <button mat-raised-button color="primary" (click)="batchApprove()" [disabled]="selectedIds().length === 0">
                Batch Approve ({{ selectedIds().length }})
              </button>
            </div>

            <table mat-table [dataSource]="tasks()" class="workflow-table">
              <ng-container matColumnDef="select">
                <th mat-header-cell *matHeaderCellDef></th>
                <td mat-cell *matCellDef="let row">
                  <mat-checkbox
                    [checked]="isSelected(row.id)"
                    (change)="toggleSelection(row.id)">
                  </mat-checkbox>
                </td>
              </ng-container>

              <ng-container matColumnDef="title">
                <th mat-header-cell *matHeaderCellDef>Title</th>
                <td mat-cell *matCellDef="let row">{{ row.title }}</td>
              </ng-container>

              <ng-container matColumnDef="type">
                <th mat-header-cell *matHeaderCellDef>Type</th>
                <td mat-cell *matCellDef="let row">{{ row.type }}</td>
              </ng-container>

              <ng-container matColumnDef="status">
                <th mat-header-cell *matHeaderCellDef>Status</th>
                <td mat-cell *matCellDef="let row">
                  {{ row.status }}
                  <span class="warn" *ngIf="row.timeoutWarning || row.escalated">ESCALATED</span>
                </td>
              </ng-container>

              <ng-container matColumnDef="actions">
                <th mat-header-cell *matHeaderCellDef>Actions</th>
                <td mat-cell *matCellDef="let row" class="action-grid">
                  <button mat-button color="primary" (click)="inspect(row.id)">View</button>
                  <button mat-button color="primary" (click)="approve(row.id)">Approve</button>
                  <button mat-button color="warn" (click)="reject(row.id)">Reject</button>
                  <button mat-button (click)="returnToSubmitter(row.id)">Return</button>
                </td>
              </ng-container>

              <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
              <tr mat-row *matRowDef="let row; columns: displayedColumns"></tr>
            </table>
          </mat-card-content>
        </mat-card>

        <mat-card *ngIf="selectedTask()">
          <mat-card-title>Task Progress</mat-card-title>
          <mat-card-content>
            <p><strong>{{ selectedTask()?.task?.title }}</strong></p>
            <mat-stepper [linear]="false" [selectedIndex]="stepIndex()">
              <mat-step *ngFor="let step of selectedTask()?.progress" [completed]="step.completed">
                <ng-template matStepLabel>{{ step.label }}</ng-template>
                <p>{{ step.active ? 'Current stage' : 'Stage snapshot' }}</p>
              </mat-step>
            </mat-stepper>
          </mat-card-content>
        </mat-card>
      </section>
    </main>
  `
})
export class DispatcherDashboardComponent implements OnInit {
  readonly tasks = signal<WorkflowTask[]>([]);
  readonly selectedTask = signal<WorkflowDetails | null>(null);
  readonly selectedIds = signal<number[]>([]);
  readonly displayedColumns = ['select', 'title', 'type', 'status', 'actions'];

  readonly createForm = this.fb.nonNullable.group({
    type: ['ROUTE_DATA_CHANGE', [Validators.required]],
    mode: ['CONDITIONAL', [Validators.required]],
    title: ['', [Validators.required]],
    payload: ['', [Validators.required]],
    requiredApprovals: [1, [Validators.required, Validators.min(1), Validators.max(10)]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly workflowService: WorkflowService
  ) {}

  ngOnInit(): void {
    this.refresh();
  }

  refresh(): void {
    this.workflowService.listTasks().subscribe({
      next: (rows) => this.tasks.set(rows)
    });
  }

  createTask(): void {
    if (this.createForm.invalid) {
      return;
    }
    const payload = this.createForm.getRawValue() as CreateWorkflowTaskRequest;
    this.workflowService.createTask(payload).subscribe({
      next: () => {
        this.createForm.patchValue({ title: '', payload: '' });
        this.refresh();
      }
    });
  }

  inspect(taskId: number): void {
    this.workflowService.getTask(taskId).subscribe({
      next: (details) => this.selectedTask.set(details)
    });
  }

  approve(taskId: number): void {
    this.workflowService.approve(taskId, 'approved by dispatcher').subscribe({
      next: () => this.refresh()
    });
  }

  reject(taskId: number): void {
    this.workflowService.reject(taskId, 'rejected by dispatcher').subscribe({
      next: () => this.refresh()
    });
  }

  returnToSubmitter(taskId: number): void {
    this.workflowService.returnToSubmitter(taskId, 'insufficient data').subscribe({
      next: () => this.refresh()
    });
  }

  evaluateEscalations(): void {
    this.workflowService.evaluateEscalations().subscribe({
      next: () => this.refresh()
    });
  }

  batchApprove(): void {
    const ids = this.selectedIds();
    if (ids.length === 0) {
      return;
    }
    this.workflowService.batchApprove(ids, 'batch approved').subscribe({
      next: () => {
        this.selectedIds.set([]);
        this.refresh();
      }
    });
  }

  toggleSelection(taskId: number): void {
    const set = new Set(this.selectedIds());
    if (set.has(taskId)) {
      set.delete(taskId);
    } else {
      set.add(taskId);
    }
    this.selectedIds.set(Array.from(set));
  }

  isSelected(taskId: number): boolean {
    return this.selectedIds().includes(taskId);
  }

  stepIndex(): number {
    const details = this.selectedTask();
    if (!details) {
      return 0;
    }
    const index = details.progress.findIndex((s) => s.active);
    return index >= 0 ? index : 0;
  }
}
