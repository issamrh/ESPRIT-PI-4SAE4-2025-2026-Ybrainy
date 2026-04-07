import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { getRealmRoles, isAuthenticated } from './keycloak.service';

export const adminGuard: CanActivateFn = () => {
  const router = inject(Router);

  if (!isAuthenticated()) {
    return router.createUrlTree(['/login'], {
      queryParams: { redirect: router.url || window.location.pathname || '/' },
    });
  }

  const roles = getRealmRoles().map((r) => r.toUpperCase());
  return roles.includes('ADMIN') || roles.includes('INSTRUCTOR');
};
