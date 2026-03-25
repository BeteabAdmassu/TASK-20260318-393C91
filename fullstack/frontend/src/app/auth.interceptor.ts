import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { NotificationService } from './notification.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const notifier = inject(NotificationService);
  const token = localStorage.getItem('mindflow_token');
  const withAuth = token
    ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : req;

  return next(withAuth).pipe(
    catchError((error) => {
      const message = error?.error?.msg || error?.error?.error || `Request failed (${error?.status ?? 'unknown'})`;
      notifier.show(message);
      return throwError(() => error);
    })
  );
};
