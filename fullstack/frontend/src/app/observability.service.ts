import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AlertDiagnostic, ObservabilitySnapshot } from './auth.models';

@Injectable({ providedIn: 'root' })
export class ObservabilityService {
  constructor(private readonly http: HttpClient) {}

  snapshot(): Observable<ObservabilitySnapshot> {
    return this.http.get<ObservabilitySnapshot>('/api/admin/observability/snapshot');
  }

  alerts(): Observable<AlertDiagnostic[]> {
    return this.http.get<AlertDiagnostic[]>('/api/admin/observability/alerts');
  }
}
