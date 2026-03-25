import { Component, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgIf } from '@angular/common';
import { NotificationPreferencesService } from './notification-preferences.service';
import { NotificationPreferencesRequest } from './auth.models';

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [ReactiveFormsModule, NgIf],
  template: `
    <main class="layout">
      <section class="card">
        <h2>Notification Preferences</h2>
        <p>Customize reminders and define a Do Not Disturb window.</p>

        <form [formGroup]="form" (ngSubmit)="save()" class="settings-form">
          <label class="toggle-row">
            <input type="checkbox" formControlName="arrivalReminderEnabled" />
            Arrival reminders
          </label>

          <label class="toggle-row">
            <input type="checkbox" formControlName="reservationSuccessEnabled" />
            Reservation success alerts
          </label>

          <label>
            Arrival reminder lead time (minutes)
            <input type="number" min="1" max="120" formControlName="reminderLeadMinutes" />
          </label>

          <label class="toggle-row">
            <input type="checkbox" formControlName="dndEnabled" />
            Enable Do Not Disturb
          </label>

          <div class="time-grid" *ngIf="form.controls.dndEnabled.value">
            <label>
              DND Start
              <input type="time" formControlName="dndStart" />
            </label>
            <label>
              DND End
              <input type="time" formControlName="dndEnd" />
            </label>
          </div>

          <button type="submit" [disabled]="form.invalid || saving()">Save Preferences</button>
        </form>

        <p class="error" *ngIf="error()">{{ error() }}</p>
        <p class="success" *ngIf="success()">{{ success() }}</p>
      </section>
    </main>
  `
})
export class SettingsComponent implements OnInit {
  readonly saving = signal(false);
  readonly error = signal('');
  readonly success = signal('');

  readonly form = this.fb.nonNullable.group({
    arrivalReminderEnabled: [true],
    reservationSuccessEnabled: [true],
    reminderLeadMinutes: [10, [Validators.required, Validators.min(1), Validators.max(120)]],
    dndEnabled: [false],
    dndStart: ['22:00'],
    dndEnd: ['07:00']
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly notificationService: NotificationPreferencesService
  ) {}

  ngOnInit(): void {
    this.notificationService.getPreferences().subscribe({
      next: (value) => {
        this.form.patchValue({
          arrivalReminderEnabled: value.arrivalReminderEnabled,
          reservationSuccessEnabled: value.reservationSuccessEnabled,
          reminderLeadMinutes: value.reminderLeadMinutes,
          dndEnabled: value.dndEnabled,
          dndStart: value.dndStart ?? '22:00',
          dndEnd: value.dndEnd ?? '07:00'
        });
      }
    });
  }

  save(): void {
    if (this.form.invalid) {
      return;
    }

    this.error.set('');
    this.success.set('');
    this.saving.set(true);

    const raw = this.form.getRawValue();
    const payload: NotificationPreferencesRequest = {
      arrivalReminderEnabled: raw.arrivalReminderEnabled,
      reservationSuccessEnabled: raw.reservationSuccessEnabled,
      reminderLeadMinutes: raw.reminderLeadMinutes,
      dndEnabled: raw.dndEnabled,
      dndStart: raw.dndEnabled ? raw.dndStart : null,
      dndEnd: raw.dndEnabled ? raw.dndEnd : null
    };

    this.notificationService.updatePreferences(payload).subscribe({
      next: (saved) => {
        this.saving.set(false);
        this.success.set(
          saved.dndActiveNow
            ? 'Saved. DND is currently active, notifications will be silenced.'
            : 'Saved. Notification rules are now active.'
        );
      },
      error: (err) => {
        this.saving.set(false);
        this.error.set(err.error?.error ?? 'Failed to save notification preferences');
      }
    });
  }
}
