import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const token = authService.getToken();

  if (!token) {
    return next(req);
  }

  // Do not attach JWT to login endpoint.
  if (req.url.includes('/api/auth/login')) {
    return next(req);
  }

  // Attach JWT only to local backend API calls.
  const isLocalApiCall =
    req.url.startsWith('/api/') ||
    /^https?:\/\/localhost(?::\d+)?\/api\//.test(req.url);

  if (!isLocalApiCall) {
    return next(req);
  }

  return next(
    req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    })
  );
};
