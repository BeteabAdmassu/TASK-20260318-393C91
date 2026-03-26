import { Component } from '@angular/core';
import { NgIf } from '@angular/common';
import { RouterOutlet } from '@angular/router';
import { NotificationService } from './notification.service';
import { LoadingService } from './loading.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, NgIf],
  template: `
    <div class="global-loading" *ngIf="loading.active()">Loading...</div>
    <div class="toast" *ngIf="notification.message()">{{ notification.message() }}</div>
    <router-outlet></router-outlet>
  `
})
export class AppComponent {
  constructor(
    public readonly notification: NotificationService,
    public readonly loading: LoadingService
  ) {}
}
