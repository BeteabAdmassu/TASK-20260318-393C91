import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { UnifiedMessage, UnifiedMessageType } from './auth.models';

@Injectable({ providedIn: 'root' })
export class MessageCenterService {
  constructor(private readonly http: HttpClient) {}

  list(type: UnifiedMessageType | 'ALL'): Observable<UnifiedMessage[]> {
    if (type === 'ALL') {
      return this.http.get<UnifiedMessage[]>('/api/passenger/messages-center');
    }
    return this.http.get<UnifiedMessage[]>('/api/passenger/messages-center', { params: { type } });
  }

  markRead(id: number, read: boolean): Observable<UnifiedMessage> {
    return this.http.put<UnifiedMessage>(`/api/passenger/messages-center/${id}/read`, { read });
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`/api/passenger/messages-center/${id}`);
  }

  createBookingEvent(payload: {
    routeNumber: string;
    passengerPhone: string;
    passengerIdCard: string;
    startTime: string;
  }): Observable<{ bookingEventId: number }> {
    return this.http.post<{ bookingEventId: number }>('/api/passenger/messages-center/booking-events', payload);
  }
}
