import { HttpInterceptorFn } from '@angular/common/http';

export const withCredentialsInterceptor: HttpInterceptorFn = (req, next) => {
  const updated = req.clone({ withCredentials: true });
  return next(updated);
};
