import { CanActivateFn } from '@angular/router';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { isAuthenticated, redirectToAppLogin } from './keycloak.service';

export const authGuard: CanActivateFn = () => {
  const router = inject(Router);

  if (isAuthenticated()) return true;

  const url = router.url || window.location.href;
  redirectToAppLogin(url);
  return false;
};
