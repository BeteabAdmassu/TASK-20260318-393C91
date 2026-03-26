import { Injectable, signal } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class LoadingService {
  readonly active = signal(false);

  begin(): void {
    this.active.set(true);
  }

  end(): void {
    this.active.set(false);
  }
}
