import { HttpEvent, HttpHandler, HttpInterceptor, HttpRequest } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { from, Observable, switchMap } from 'rxjs';
import { getToken, updateToken } from './keycloak.service';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  intercept(req: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    return from(updateToken().catch(() => undefined)).pipe(
      switchMap(() => {
      const token = getToken();
      if (!token) return next.handle(req);

        return next.handle(
          req.clone({
            setHeaders: { Authorization: `Bearer ${token}` },
          })
        );
      })
    );
  }
}
