import { CanActivateFn } from '@angular/router';
import { getRealmRoles, isAuthenticated, redirectToAppLogin } from './keycloak.service';

export const adminGuard: CanActivateFn = () => {
  if (!isAuthenticated()) {
    redirectToAppLogin(window.location.href);
    return false;
  }

  return getRealmRoles().map((r) => r.toUpperCase()).includes('ADMIN');
};
