import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { authGuard } from './auth.guard';
import { roleGuard } from './role.guard';
import { AuthService } from './auth.service';

class AuthServiceStub {
  authenticated = false;
  roleOk = false;

  isAuthenticated(): boolean {
    return this.authenticated;
  }

  hasAnyRole(): boolean {
    return this.roleOk;
  }
}

describe('Route Guards', () => {
  let auth: AuthServiceStub;

  beforeEach(() => {
    auth = new AuthServiceStub();
    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: auth },
        {
          provide: Router,
          useValue: {
            createUrlTree: (commands: unknown[]) => ({ commands })
          }
        }
      ]
    });
  });

  it('authGuard blocks anonymous access', () => {
    auth.authenticated = false;
    const result = TestBed.runInInjectionContext(() =>
      authGuard({} as never, { url: '/admin' } as never)
    ) as unknown as { commands: unknown[] };

    expect(Array.isArray(result.commands)).toBeTrue();
  });

  it('roleGuard allows configured role', () => {
    auth.roleOk = true;
    const result = TestBed.runInInjectionContext(() =>
      roleGuard({ data: { roles: ['ADMIN'] } } as never, {} as never)
    );

    expect(result).toBeTrue();
  });
});
