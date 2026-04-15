import { HttpInterceptorFn } from '@angular/common/http';
import { environment } from '@env/environment';

export const apiPrefixInterceptor: HttpInterceptorFn = (req, next) => {
  const url = req.url ?? '';

  // Only prefix same-origin relative API and uploads calls.
  if (url.startsWith('/api/') || url === '/api' || url.startsWith('/uploads/')) {
    const prefixed = environment.apiUrl.replace(/\/$/, '') + url;
    return next(req.clone({ url: prefixed }));
  }

  return next(req);
};
