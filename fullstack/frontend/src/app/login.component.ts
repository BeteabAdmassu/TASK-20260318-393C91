import { Component } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { NgIf } from '@angular/common';
import { AuthService } from './auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [ReactiveFormsModule, NgIf],
  template: `
    <main class="layout">
      <section class="card">
        <h1>MindFlow Access Gateway</h1>
        <p>Offline LAN authentication for Passenger, Dispatcher, and Admin users.</p>

        <form [formGroup]="form" (ngSubmit)="submit()">
          <label>
            Username
            <input type="text" formControlName="username" />
          </label>

          <label>
            Password
            <input type="password" formControlName="password" />
          </label>

          <small>Password must be at least 8 characters.</small>

          <button type="submit" [disabled]="form.invalid || loading">Login</button>
          <p class="error" *ngIf="error">{{ error }}</p>
        </form>

        <div class="hint">
          Bootstrap credentials are environment-configured for secure deployment.
        </div>
      </section>
    </main>
  `
})
export class LoginComponent {
  loading = false;
  error = '';

  readonly form = this.fb.nonNullable.group({
    username: ['', [Validators.required]],
    password: ['', [Validators.required, Validators.minLength(8)]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly authService: AuthService,
    private readonly router: Router
  ) {}

  submit(): void {
    if (this.form.invalid) {
      return;
    }
    this.loading = true;
    this.error = '';

    this.authService.login(this.form.getRawValue()).subscribe({
      next: () => {
        this.loading = false;
        this.router.navigateByUrl('/dashboard');
      },
      error: () => {
        this.loading = false;
        this.error = 'Invalid username or password';
      }
    });
  }
}
