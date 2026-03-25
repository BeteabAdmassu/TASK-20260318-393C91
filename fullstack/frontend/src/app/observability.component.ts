import { Component, OnDestroy, OnInit, signal } from '@angular/core';
import { NgFor, NgIf } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { interval, startWith, Subject, switchMap, takeUntil } from 'rxjs';
import { AlertDiagnostic, ObservabilitySnapshot } from './auth.models';
import { ObservabilityService } from './observability.service';

@Component({
  selector: 'app-observability',
  standalone: true,
  imports: [NgIf, NgFor, MatCardModule, MatButtonModule],
  template: `
    <main class="layout">
      <section class="card workflow-shell">
        <h2>Observability & Health Monitoring</h2>

        <mat-card *ngIf="snapshot() as s">
          <mat-card-title>System Snapshot</mat-card-title>
          <mat-card-content>
            <div class="results">
              <div class="row"><strong>Queue Backlog</strong>: {{ s.queueBacklog }}</div>
              <div class="row"><strong>API P95</strong>: {{ s.apiP95Ms }} ms</div>
              <div class="row"><strong>Search P95</strong>: {{ s.searchP95Ms }} ms</div>
              <div class="row"><strong>Parsing P95</strong>: {{ s.parsingP95Ms }} ms</div>
              <div class="row"><strong>Queue P95</strong>: {{ s.queueP95Ms }} ms</div>
            </div>
            <div class="meta">Health: {{ s.healthEndpoint }} | Metrics: {{ s.metricsEndpoint }}</div>
          </mat-card-content>
        </mat-card>

        <mat-card>
          <mat-card-title>Diagnostics Alerts</mat-card-title>
          <mat-card-content>
            <button mat-stroked-button (click)="loadOnce()">Refresh Now</button>
            <div *ngIf="alerts().length === 0" class="meta">No alerts recorded.</div>
            <div *ngFor="let alert of alerts()" class="message-item" [class.unread]="alert.severity === 'WARN'">
              <div class="message-head">
                <strong>{{ alert.alertType }}</strong>
                <span class="meta">{{ alert.severity }} | {{ alert.createdAt }}</span>
              </div>
              <p>{{ alert.message }}</p>
            </div>
          </mat-card-content>
        </mat-card>
      </section>
    </main>
  `
})
export class ObservabilityComponent implements OnInit, OnDestroy {
  readonly snapshot = signal<ObservabilitySnapshot | null>(null);
  readonly alerts = signal<AlertDiagnostic[]>([]);

  private readonly destroy$ = new Subject<void>();

  constructor(private readonly service: ObservabilityService) {}

  ngOnInit(): void {
    interval(15000)
      .pipe(
        startWith(0),
        switchMap(() => this.service.snapshot()),
        takeUntil(this.destroy$)
      )
      .subscribe({ next: (s) => this.snapshot.set(s) });

    interval(15000)
      .pipe(
        startWith(0),
        switchMap(() => this.service.alerts()),
        takeUntil(this.destroy$)
      )
      .subscribe({ next: (rows) => this.alerts.set(rows) });
  }

  loadOnce(): void {
    this.service.snapshot().subscribe({ next: (s) => this.snapshot.set(s) });
    this.service.alerts().subscribe({ next: (rows) => this.alerts.set(rows) });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
