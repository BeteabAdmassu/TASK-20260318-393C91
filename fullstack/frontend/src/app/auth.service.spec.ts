import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  beforeEach(() => {
    localStorage.removeItem('mindflow_token');
    localStorage.removeItem('mindflow_role');
    localStorage.removeItem('mindflow_user');
    TestBed.configureTestingModule({ imports: [HttpClientTestingModule] });
  });

  it('reports authenticated when token exists', () => {
    localStorage.setItem('mindflow_token', 'token-value');
    localStorage.setItem('mindflow_role', 'PASSENGER');
    localStorage.setItem('mindflow_user', 'u1');
    const service = TestBed.inject(AuthService);
    expect(service.isAuthenticated()).toBeTrue();
  });

  it('checks role membership correctly', () => {
    localStorage.setItem('mindflow_token', 'token-value');
    localStorage.setItem('mindflow_role', 'ADMIN');
    localStorage.setItem('mindflow_user', 'admin1');
    const service = TestBed.inject(AuthService);
    expect(service.hasAnyRole(['ADMIN'])).toBeTrue();
    expect(service.hasAnyRole(['PASSENGER', 'DISPATCHER'])).toBeFalse();
  });
});
