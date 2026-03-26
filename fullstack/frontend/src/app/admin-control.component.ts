import { Component, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgFor } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatTableModule } from '@angular/material/table';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { AdminControlService } from './admin-control.service';
import { DictionaryItem, RuleWeights, TemplateConfig } from './auth.models';

@Component({
  selector: 'app-admin-control',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    NgFor,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatTableModule,
    MatCheckboxModule
  ],
  template: `
    <main class="layout">
      <section class="card workflow-shell">
        <h2>Administrator Control Panel</h2>

        <mat-card>
          <mat-card-title>Search Rule Weights</mat-card-title>
          <mat-card-content>
            <form [formGroup]="weightsForm" class="create-grid" (ngSubmit)="saveWeights()">
              <mat-form-field appearance="outline">
                <mat-label>Relevance Weight</mat-label>
                <input matInput type="number" formControlName="relevanceWeight" />
              </mat-form-field>
              <mat-form-field appearance="outline">
                <mat-label>Frequency Weight</mat-label>
                <input matInput type="number" formControlName="frequencyWeight" />
              </mat-form-field>
              <mat-form-field appearance="outline">
                <mat-label>Popularity Weight</mat-label>
                <input matInput type="number" formControlName="popularityWeight" />
              </mat-form-field>
              <mat-form-field appearance="outline">
                <mat-label>Ranking Mode</mat-label>
                <input matInput formControlName="rankingMode" placeholder="BLENDED or STRICT_FREQUENCY_POPULARITY" />
              </mat-form-field>
              <button mat-raised-button color="primary" type="submit">Save Weights</button>
            </form>
          </mat-card-content>
        </mat-card>

        <mat-card>
          <mat-card-title>Notification Templates</mat-card-title>
          <mat-card-content>
            <form [formGroup]="templateForm" class="create-grid" (ngSubmit)="saveTemplate()">
              <mat-form-field appearance="outline"><mat-label>Key</mat-label><input matInput formControlName="templateKey" /></mat-form-field>
              <mat-form-field appearance="outline"><mat-label>Subject</mat-label><input matInput formControlName="subject" /></mat-form-field>
              <mat-form-field appearance="outline"><mat-label>Body</mat-label><input matInput formControlName="body" /></mat-form-field>
              <button mat-raised-button color="primary" type="submit">Save Template</button>
            </form>

            <table mat-table [dataSource]="templates()" class="workflow-table">
              <ng-container matColumnDef="templateKey"><th mat-header-cell *matHeaderCellDef>Key</th><td mat-cell *matCellDef="let row">{{ row.templateKey }}</td></ng-container>
              <ng-container matColumnDef="subject"><th mat-header-cell *matHeaderCellDef>Subject</th><td mat-cell *matCellDef="let row">{{ row.subject }}</td></ng-container>
              <ng-container matColumnDef="body"><th mat-header-cell *matHeaderCellDef>Body</th><td mat-cell *matCellDef="let row">{{ row.body }}</td></ng-container>
              <ng-container matColumnDef="actions">
                <th mat-header-cell *matHeaderCellDef>Actions</th>
                <td mat-cell *matCellDef="let row" class="action-grid">
                  <button mat-button (click)="editTemplate(row)">Edit</button>
                  <button mat-button color="warn" (click)="deleteTemplate(row.id)">Delete</button>
                </td>
              </ng-container>
              <tr mat-header-row *matHeaderRowDef="templateCols"></tr>
              <tr mat-row *matRowDef="let row; columns: templateCols"></tr>
            </table>
          </mat-card-content>
        </mat-card>

        <mat-card>
          <mat-card-title>Field Dictionaries</mat-card-title>
          <mat-card-content>
            <form [formGroup]="dictForm" class="create-grid" (ngSubmit)="saveDictionary()">
              <mat-form-field appearance="outline"><mat-label>Category</mat-label><input matInput formControlName="category" /></mat-form-field>
              <mat-form-field appearance="outline"><mat-label>Code</mat-label><input matInput formControlName="code" /></mat-form-field>
              <mat-form-field appearance="outline"><mat-label>Value</mat-label><input matInput formControlName="value" /></mat-form-field>
              <label class="toggle-row"><input type="checkbox" formControlName="enabled" /> Enabled</label>
              <button mat-raised-button color="primary" type="submit">Save Dictionary</button>
            </form>

            <table mat-table [dataSource]="dictionary()" class="workflow-table">
              <ng-container matColumnDef="category"><th mat-header-cell *matHeaderCellDef>Category</th><td mat-cell *matCellDef="let row">{{ row.category }}</td></ng-container>
              <ng-container matColumnDef="code"><th mat-header-cell *matHeaderCellDef>Code</th><td mat-cell *matCellDef="let row">{{ row.code }}</td></ng-container>
              <ng-container matColumnDef="value"><th mat-header-cell *matHeaderCellDef>Value</th><td mat-cell *matCellDef="let row">{{ row.value }}</td></ng-container>
              <ng-container matColumnDef="enabled"><th mat-header-cell *matHeaderCellDef>Enabled</th><td mat-cell *matCellDef="let row">{{ row.enabled }}</td></ng-container>
              <ng-container matColumnDef="actions">
                <th mat-header-cell *matHeaderCellDef>Actions</th>
                <td mat-cell *matCellDef="let row" class="action-grid">
                  <button mat-button (click)="editDictionary(row)">Edit</button>
                  <button mat-button color="warn" (click)="deleteDictionary(row.id)">Delete</button>
                </td>
              </ng-container>
              <tr mat-header-row *matHeaderRowDef="dictCols"></tr>
              <tr mat-row *matRowDef="let row; columns: dictCols"></tr>
            </table>
          </mat-card-content>
        </mat-card>
      </section>
    </main>
  `
})
export class AdminControlComponent implements OnInit {
  readonly templates = signal<TemplateConfig[]>([]);
  readonly dictionary = signal<DictionaryItem[]>([]);
  readonly templateCols = ['templateKey', 'subject', 'body', 'actions'];
  readonly dictCols = ['category', 'code', 'value', 'enabled', 'actions'];

  private editingTemplateId: number | null = null;
  private editingDictionaryId: number | null = null;

  readonly weightsForm = this.fb.nonNullable.group({
    relevanceWeight: [1000000, [Validators.required, Validators.min(1)]],
    frequencyWeight: [1000, [Validators.required, Validators.min(1)]],
    popularityWeight: [1, [Validators.required, Validators.min(1)]],
    rankingMode: this.fb.nonNullable.control<'BLENDED' | 'STRICT_FREQUENCY_POPULARITY'>('BLENDED', [Validators.required])
  });

  readonly templateForm = this.fb.nonNullable.group({
    templateKey: ['', Validators.required],
    subject: ['', Validators.required],
    body: ['', Validators.required]
  });

  readonly dictForm = this.fb.nonNullable.group({
    category: ['', Validators.required],
    code: ['', Validators.required],
    value: ['', Validators.required],
    enabled: [true]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly service: AdminControlService
  ) { }

  ngOnInit(): void {
    this.loadAll();
  }

  saveWeights(): void {
    const payload = this.weightsForm.getRawValue() as RuleWeights;
    this.service.updateWeights(payload).subscribe({ next: (w) => this.weightsForm.patchValue(w) });
  }

  saveTemplate(): void {
    const payload = this.templateForm.getRawValue();
    const request = { templateKey: payload.templateKey, subject: payload.subject, body: payload.body };
    const call = this.editingTemplateId == null
      ? this.service.createTemplate(request)
      : this.service.updateTemplate(this.editingTemplateId, request);
    call.subscribe({ next: () => { this.editingTemplateId = null; this.templateForm.reset(); this.loadTemplates(); } });
  }

  editTemplate(row: TemplateConfig): void {
    this.editingTemplateId = row.id;
    this.templateForm.patchValue({ templateKey: row.templateKey, subject: row.subject, body: row.body });
  }

  deleteTemplate(id: number): void {
    this.service.deleteTemplate(id).subscribe({ next: () => this.loadTemplates() });
  }

  saveDictionary(): void {
    const payload = this.dictForm.getRawValue();
    const request = { category: payload.category, code: payload.code, value: payload.value, enabled: payload.enabled };
    const call = this.editingDictionaryId == null
      ? this.service.createDictionary(request)
      : this.service.updateDictionary(this.editingDictionaryId, request);
    call.subscribe({ next: () => { this.editingDictionaryId = null; this.dictForm.reset({ enabled: true }); this.loadDictionary(); } });
  }

  editDictionary(row: DictionaryItem): void {
    this.editingDictionaryId = row.id;
    this.dictForm.patchValue({ category: row.category, code: row.code, value: row.value, enabled: row.enabled });
  }

  deleteDictionary(id: number): void {
    this.service.deleteDictionary(id).subscribe({ next: () => this.loadDictionary() });
  }

  private loadAll(): void {
    this.service.getWeights().subscribe({ next: (w) => this.weightsForm.patchValue(w) });
    this.loadTemplates();
    this.loadDictionary();
  }

  private loadTemplates(): void {
    this.service.listTemplates().subscribe({ next: (rows) => this.templates.set(rows) });
  }

  private loadDictionary(): void {
    this.service.listDictionary().subscribe({ next: (rows) => this.dictionary.set(rows) });
  }
}
