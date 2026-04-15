import { HttpInterceptorFn } from '@angular/common/http';
import { environment } from '@env/environment';

export const apiPrefixInterceptor: HttpInterceptorFn = (req, next) => {
  const url = req.url ?? '';

  // Only prefix same-origin relative API and uploads calls.
  if (url.startsWith('/api/') || url === '/api' || url.startsWith('/uploads/')) {
    const courseApiPrefixes = [
      '/api/courses',
      '/api/quizzes',
      '/api/enrollments',
      '/api/payments',
      '/api/learning-paths',
      '/api/instructor',
      '/api/students',
      '/api/ml',
      '/api/certificates',
    ];
    const targetBase = courseApiPrefixes.some((prefix) => url === prefix || url.startsWith(`${prefix}/`))
      ? environment.apiBaseUrl
      : environment.forumApiUrl;
    const prefixed = targetBase.replace(/\/$/, '') + url;
    return next(req.clone({ url: prefixed }));
  }

  return next(req);
};
