import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { isAuthenticated } from './keycloak.service';

export const loginRequiredGuard: CanActivateFn = () => {
  const router = inject(Router);

  if (!isAuthenticated()) {
    return router.createUrlTree(['/login'], {
      queryParams: { redirect: router.url || window.location.pathname || '/' },
    });
  }

  return true;
};
