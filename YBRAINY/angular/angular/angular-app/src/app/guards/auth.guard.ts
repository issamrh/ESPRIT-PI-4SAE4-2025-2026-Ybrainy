import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthMockService } from '../frontoffice/services/auth-mock.service';

export const authGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthMockService);
  const router = inject(Router);

  const role = auth.getRole();
  const url = state.url;

  // STUDENT trying to access /dashboard → redirect to /courses
  if (url.startsWith('/dashboard') && role === 'STUDENT') {
    return router.createUrlTree(['/courses']);
  }

  // INSTRUCTOR or ADMIN → allow access to everything
  // null role → allow through
  return true;
};
