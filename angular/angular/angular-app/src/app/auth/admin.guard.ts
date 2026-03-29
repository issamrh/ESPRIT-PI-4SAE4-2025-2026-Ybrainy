import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { getRealmRoles, isAuthenticated, redirectToAppLogin } from './keycloak.service';

export const adminGuard: CanActivateFn = (_route, state) => {
  const router = inject(Router);

  if (!isAuthenticated()) {
    redirectToAppLogin(state.url || window.location.href);
    return false;
  }

  const roles = getRealmRoles().map((r) => r.toUpperCase());
  if (roles.includes('ADMIN') || roles.includes('INSTRUCTOR')) {
    return true;
  }

  return router.createUrlTree(['/courses']);
};
