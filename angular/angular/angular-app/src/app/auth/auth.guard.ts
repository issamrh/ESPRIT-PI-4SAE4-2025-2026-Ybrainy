import { CanActivateFn } from '@angular/router';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { isAuthenticated, redirectToAppLogin } from './keycloak.service';
import { UserSessionService } from '../tracking/user-session.service';

export const authGuard: CanActivateFn = () => {
  const router = inject(Router);
  const userSession = inject(UserSessionService);

  console.log('[AUTH GUARD] Running...');
  console.log('[AUTH GUARD] isAuthenticated:', isAuthenticated());

  if (!isAuthenticated()) {
    const url = router.url || window.location.href;
    console.log('[AUTH GUARD] Not authenticated, redirecting to login');
    redirectToAppLogin(url);
    return false;
  }

  // Set default mode on first login (if not already set)
  const userRole = userSession.get()?.role;
  const storedMode = localStorage.getItem('ybrainy_user_mode');

  console.log('[AUTH GUARD] User role:', userRole);
  console.log('[AUTH GUARD] Stored mode:', storedMode);
  console.log('[AUTH GUARD] User session:', userSession.get());

  if (!storedMode && userRole) {
    console.log('[AUTH GUARD] First login detected, setting mode to:', userRole);
    // First time - set mode to role
    userSession.setMode(userRole as 'STUDENT' | 'INSTRUCTOR' | 'ADMIN');

    // Route to appropriate default page
    if (userRole === 'ADMIN') {
      console.log('[AUTH GUARD] Redirecting to /dashboard');
      return router.parseUrl('/dashboard');
    } else if (userRole === 'INSTRUCTOR') {
      console.log('[AUTH GUARD] Redirecting to /dashboard/courses');
      return router.parseUrl('/dashboard/courses');
    }
    console.log('[AUTH GUARD] Student, allowing through');
  }

  console.log('[AUTH GUARD] Returning true (allowing access)');
  return true;
};
