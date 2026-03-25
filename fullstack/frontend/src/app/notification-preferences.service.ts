import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  NotificationPreferences,
  NotificationPreferencesRequest
} from './auth.models';

@Injectable({ providedIn: 'root' })
export class NotificationPreferencesService {
  constructor(private readonly http: HttpClient) {}

  getPreferences(): Observable<NotificationPreferences> {
    return this.http.get<NotificationPreferences>('/api/passenger/preferences');
  }

  updatePreferences(
    payload: NotificationPreferencesRequest
  ): Observable<NotificationPreferences> {
    return this.http.put<NotificationPreferences>('/api/passenger/preferences', payload);
  }
}
