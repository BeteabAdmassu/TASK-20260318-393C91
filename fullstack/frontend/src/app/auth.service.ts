import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../environments/environment';
import { LoginRequest, LoginResponse } from './auth.models';

const TOKEN_KEY = 'mindflow_token';
const ROLE_KEY = 'mindflow_role';
const USER_KEY = 'mindflow_user';

@Injectable({ providedIn: 'root' })
export class AuthService {
  readonly token = signal<string | null>(localStorage.getItem(TOKEN_KEY));
  readonly role = signal<string | null>(localStorage.getItem(ROLE_KEY));
  readonly username = signal<string | null>(localStorage.getItem(USER_KEY));

  constructor(private readonly http: HttpClient) {}

  login(payload: LoginRequest): Observable<LoginResponse> {
    return this.http
      .post<LoginResponse>(`${environment.apiBaseUrl}/auth/login`, payload)
      .pipe(tap((res) => this.persistAuth(res)));
  }

  logout(): void {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(ROLE_KEY);
    localStorage.removeItem(USER_KEY);
    this.token.set(null);
    this.role.set(null);
    this.username.set(null);
  }

  private persistAuth(response: LoginResponse): void {
    localStorage.setItem(TOKEN_KEY, response.token);
    localStorage.setItem(ROLE_KEY, response.role);
    localStorage.setItem(USER_KEY, response.username);
    this.token.set(response.token);
    this.role.set(response.role);
    this.username.set(response.username);
  }
}
