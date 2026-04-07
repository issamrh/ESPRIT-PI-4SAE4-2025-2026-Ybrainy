import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { getRealmRoles, isAuthenticated } from './keycloak.service';

export const frontofficeUserGuard: CanActivateFn = () => {
  const router = inject(Router);

  if (!isAuthenticated()) {
    return router.createUrlTree(['/login'], {
      queryParams: { redirect: router.url || window.location.pathname || '/' },
    });
  }

  const isAdmin = getRealmRoles().some((role) => String(role).toUpperCase() === 'ADMIN');
  if (isAdmin) {
    return router.createUrlTree(['/dashboard']);
  }

  return true;
};
