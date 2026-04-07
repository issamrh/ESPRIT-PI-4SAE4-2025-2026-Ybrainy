import { HttpInterceptorFn } from '@angular/common/http';
import { from, switchMap } from 'rxjs';
import { getToken, updateToken } from './keycloak.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  return from(updateToken().catch(() => undefined)).pipe(
    switchMap(() => {
      const token = getToken();
      if (!token) return next(req);

      return next(
        req.clone({
          setHeaders: { Authorization: `Bearer ${token}` },
        })
      );
    })
  );
};
