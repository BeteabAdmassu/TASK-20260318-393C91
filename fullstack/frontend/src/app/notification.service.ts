import { Injectable, signal } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class NotificationService {
  readonly message = signal<string>('');

  show(text: string): void {
    this.message.set(text);
    setTimeout(() => {
      if (this.message() === text) {
        this.message.set('');
      }
    }, 3500);
  }
}
