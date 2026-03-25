import { Component, OnDestroy, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgFor, NgIf } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { Subject, interval, startWith, switchMap, takeUntil } from 'rxjs';
import { UnifiedMessage, UnifiedMessageType } from './auth.models';
import { MessageCenterService } from './message-center.service';
import { maskSensitiveInUi } from './masking.util';

type MessageFilter = 'ALL' | UnifiedMessageType;

@Component({
  selector: 'app-message-center',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    NgIf,
    NgFor,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule
  ],
  template: `
    <main class="layout">
      <section class="card workflow-shell">
        <h2>Unified Message Center</h2>

        <mat-card>
          <mat-card-title>Quick Booking Event Seed</mat-card-title>
          <mat-card-content>
            <form [formGroup]="bookingForm" class="create-grid" (ngSubmit)="seedBooking()">
              <mat-form-field appearance="outline">
                <mat-label>Route Number</mat-label>
                <input matInput formControlName="routeNumber" />
              </mat-form-field>

              <mat-form-field appearance="outline">
                <mat-label>Passenger Phone</mat-label>
                <input matInput formControlName="passengerPhone" />
              </mat-form-field>

              <mat-form-field appearance="outline">
                <mat-label>Passenger ID</mat-label>
                <input matInput formControlName="passengerIdCard" />
              </mat-form-field>

              <mat-form-field appearance="outline">
                <mat-label>Start Time</mat-label>
                <input matInput type="datetime-local" formControlName="startTime" />
              </mat-form-field>

              <button mat-raised-button color="primary" type="submit" [disabled]="bookingForm.invalid">
                Create Event
              </button>
            </form>
          </mat-card-content>
        </mat-card>

        <mat-card>
          <mat-card-title>Inbox</mat-card-title>
          <mat-card-content>
            <div class="toolbar">
              <mat-form-field appearance="outline">
                <mat-label>Filter</mat-label>
                <mat-select [value]="filter()" (valueChange)="setFilter($event)">
                  <mat-option value="ALL">All</mat-option>
                  <mat-option value="RESERVATION_SUCCESS">Reservation Success</mat-option>
                  <mat-option value="ARRIVAL_REMINDER">Arrival Reminder</mat-option>
                  <mat-option value="MISSED_CHECK_IN">Missed Check-In</mat-option>
                </mat-select>
              </mat-form-field>
              <button mat-stroked-button (click)="load()">Refresh</button>
            </div>

            <div *ngIf="messages().length === 0" class="meta">No messages yet.</div>

            <div *ngFor="let message of messages()" class="message-item" [class.unread]="!message.read">
              <div class="message-head">
                <div>
                  <strong>{{ message.title }}</strong>
                  <span class="meta">{{ message.type }} | {{ message.createdAt }}</span>
                </div>
                <div class="actions">
                  <button mat-button (click)="toggleRead(message)">{{ message.read ? 'Mark Unread' : 'Mark Read' }}</button>
                  <button mat-button color="warn" (click)="remove(message.id)">Delete</button>
                </div>
              </div>
              <p>{{ maskedContent(message) }}</p>
            </div>
          </mat-card-content>
        </mat-card>
      </section>
    </main>
  `
})
export class MessageCenterComponent implements OnInit, OnDestroy {
  readonly filter = signal<MessageFilter>('ALL');
  readonly messages = signal<UnifiedMessage[]>([]);

  readonly bookingForm = this.fb.nonNullable.group({
    routeNumber: ['1A', [Validators.required]],
    passengerPhone: ['13812345678', [Validators.required]],
    passengerIdCard: ['ABC123456789XYZ', [Validators.required]],
    startTime: ['', [Validators.required]]
  });

  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly fb: FormBuilder,
    private readonly service: MessageCenterService
  ) {}

  ngOnInit(): void {
    interval(15000)
      .pipe(
        startWith(0),
        switchMap(() => this.service.list(this.filter())),
        takeUntil(this.destroy$)
      )
      .subscribe({
        next: (rows) => this.messages.set(rows)
      });
  }

  setFilter(next: MessageFilter): void {
    this.filter.set(next);
    this.load();
  }

  load(): void {
    this.service.list(this.filter()).subscribe({
      next: (rows) => this.messages.set(rows)
    });
  }

  toggleRead(message: UnifiedMessage): void {
    this.service.markRead(message.id, !message.read).subscribe({
      next: () => this.load()
    });
  }

  remove(id: number): void {
    this.service.delete(id).subscribe({
      next: () => this.load()
    });
  }

  maskedContent(message: UnifiedMessage): string {
    return maskSensitiveInUi(message.content);
  }

  seedBooking(): void {
    if (this.bookingForm.invalid) {
      return;
    }
    const value = this.bookingForm.getRawValue();
    const isoStart = new Date(value.startTime).toISOString();
    this.service.createBookingEvent({
      routeNumber: value.routeNumber,
      passengerPhone: value.passengerPhone,
      passengerIdCard: value.passengerIdCard,
      startTime: isoStart
    }).subscribe({
      next: () => this.load()
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
