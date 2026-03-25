import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';
import { Role } from './auth.models';

export const roleGuard: CanActivateFn = (route) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const allowed = (route.data?.['roles'] ?? []) as Role[];
  if (allowed.length === 0 || auth.hasAnyRole(allowed)) {
    return true;
  }
  return router.createUrlTree(['/dashboard']);
};
