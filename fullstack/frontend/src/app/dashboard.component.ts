import { Component, OnDestroy, signal } from '@angular/core';
import { NgFor, NgIf } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { AuthService } from './auth.service';
import { FormBuilder, FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { SearchService } from './search.service';
import { SearchResponse, SearchResult } from './auth.models';
import { Subject, debounceTime, distinctUntilChanged, switchMap, takeUntil } from 'rxjs';
import { Router } from '@angular/router';
import { NotificationPreferencesService } from './notification-preferences.service';
import { NotificationPreferencesRequest } from './auth.models';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [NgIf, NgFor, ReactiveFormsModule],
  template: `
    <main class="layout">
      <section class="card">
        <h2>Authenticated Session</h2>
        <p>User: <strong>{{ authService.username() }}</strong></p>
        <p>Role: <strong>{{ authService.role() }}</strong></p>

        <section class="search-box">
          <h3>Smart Passenger Search</h3>
          <p>Search by route number, stop name, keyword, or pinyin initials (e.g., bj).</p>
          <input [formControl]="queryControl" placeholder="Try: bj, airport, 1A" />

          <div class="suggestions" *ngIf="suggestions().length > 0">
            <span class="chip" *ngFor="let suggestion of suggestions()" (click)="pickSuggestion(suggestion)">
              {{ suggestion }}
            </span>
          </div>

          <div class="results" *ngIf="results().length > 0">
            <div class="row" *ngFor="let item of results()">
              <div>
                <strong>{{ item.routeNumber }}</strong> - {{ item.stopName }}
              </div>
              <div class="meta">
                match={{ item.matchType }}, freq={{ item.frequencyPriority }}, pop={{ item.stopPopularity }}
              </div>
            </div>
          </div>

          <section class="settings-form compact-settings">
            <h3>Quick Reminder Preferences</h3>
            <form [formGroup]="quickPrefForm" (ngSubmit)="saveQuickPreferences()">
              <label class="toggle-row">
                <input type="checkbox" formControlName="arrivalReminderEnabled" />
                Arrival reminders
              </label>
              <label>
                Reminder lead (minutes)
                <input type="number" min="1" max="120" formControlName="reminderLeadMinutes" />
              </label>
              <label class="toggle-row">
                <input type="checkbox" formControlName="dndEnabled" />
                Enable DND
              </label>
              <div class="time-grid" *ngIf="quickPrefForm.controls.dndEnabled.value">
                <label>
                  DND Start
                  <input type="time" formControlName="dndStart" />
                </label>
                <label>
                  DND End
                  <input type="time" formControlName="dndEnd" />
                </label>
              </div>
              <button type="submit" [disabled]="quickPrefForm.invalid">Save Quick Preferences</button>
            </form>
          </section>
        </section>

        <div class="actions">
          <button (click)="probe('/api/passenger/ping')">Passenger Scope</button>
          <button (click)="probe('/api/dispatcher/ping')">Dispatcher Scope</button>
          <button (click)="probe('/api/admin/ping')">Admin Scope</button>
          <button (click)="goToDispatcher()">Dispatcher Workflow</button>
          <button (click)="goToSettings()">Notification Settings</button>
          <button (click)="goToMessages()">Message Center</button>
          <button (click)="goToAdmin()">Admin Control</button>
          <button (click)="goToObservability()">Observability</button>
          <button (click)="logout()">Logout</button>
        </div>

        <pre *ngIf="result()">{{ result() }}</pre>
      </section>
    </main>
  `
})
export class DashboardComponent implements OnDestroy {
  readonly result = signal('');
  readonly suggestions = signal<string[]>([]);
  readonly results = signal<SearchResult[]>([]);
  readonly queryControl = new FormControl('', { nonNullable: true });
  readonly quickPrefForm = this.fb.nonNullable.group({
    arrivalReminderEnabled: [true],
    reservationSuccessEnabled: [true],
    reminderLeadMinutes: [10, [Validators.required, Validators.min(1), Validators.max(120)]],
    dndEnabled: [false],
    dndStart: ['22:00'],
    dndEnd: ['07:00']
  });

  private readonly destroy$ = new Subject<void>();

  constructor(
    public readonly authService: AuthService,
    private readonly fb: FormBuilder,
    private readonly http: HttpClient,
    private readonly searchService: SearchService,
    private readonly router: Router,
    private readonly notificationPreferencesService: NotificationPreferencesService
  ) {
    this.queryControl.valueChanges
      .pipe(
        debounceTime(250),
        distinctUntilChanged(),
        switchMap((query) => this.searchService.search(query.trim())),
        takeUntil(this.destroy$)
      )
      .subscribe({
        next: (response) => this.applySearchResponse(response),
        error: () => {
          this.suggestions.set([]);
          this.results.set([]);
        }
      });

    this.searchService.search('').pipe(takeUntil(this.destroy$)).subscribe({
      next: (response) => this.applySearchResponse(response)
    });

    this.notificationPreferencesService.getPreferences().pipe(takeUntil(this.destroy$)).subscribe({
      next: (prefs) => {
        this.quickPrefForm.patchValue({
          arrivalReminderEnabled: prefs.arrivalReminderEnabled,
          reservationSuccessEnabled: prefs.reservationSuccessEnabled,
          reminderLeadMinutes: prefs.reminderLeadMinutes,
          dndEnabled: prefs.dndEnabled,
          dndStart: prefs.dndStart ?? '22:00',
          dndEnd: prefs.dndEnd ?? '07:00'
        });
      }
    });
  }

  pickSuggestion(suggestion: string): void {
    const value = suggestion.split(' - ')[0] ?? suggestion;
    this.queryControl.setValue(value);
  }

  probe(path: string): void {
    this.http.get(path).subscribe({
      next: (res) => this.result.set(JSON.stringify(res, null, 2)),
      error: (err) => this.result.set(JSON.stringify(err.error ?? err, null, 2))
    });
  }

  logout(): void {
    this.authService.logout();
    this.result.set('Logged out');
  }

  goToSettings(): void {
    this.router.navigateByUrl('/settings');
  }

  goToDispatcher(): void {
    this.router.navigateByUrl('/dispatcher');
  }

  goToMessages(): void {
    this.router.navigateByUrl('/messages');
  }

  goToAdmin(): void {
    this.router.navigateByUrl('/admin');
  }

  goToObservability(): void {
    this.router.navigateByUrl('/observability');
  }

  saveQuickPreferences(): void {
    if (this.quickPrefForm.invalid) {
      return;
    }
    const raw = this.quickPrefForm.getRawValue();
    const payload: NotificationPreferencesRequest = {
      arrivalReminderEnabled: raw.arrivalReminderEnabled,
      reservationSuccessEnabled: raw.reservationSuccessEnabled,
      reminderLeadMinutes: raw.reminderLeadMinutes,
      dndEnabled: raw.dndEnabled,
      dndStart: raw.dndEnabled ? raw.dndStart : null,
      dndEnd: raw.dndEnabled ? raw.dndEnd : null
    };
    this.notificationPreferencesService.updatePreferences(payload).subscribe({
      next: () => this.result.set('Quick preferences saved'),
      error: () => this.result.set('Failed to save preferences')
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private applySearchResponse(response: SearchResponse): void {
    this.suggestions.set(response.suggestions);
    this.results.set(response.results);
  }
}
