import {HttpErrorResponse, HttpInterceptorFn} from '@angular/common/http';
import {inject} from "@angular/core";
import {AuthService} from "./auth.service";
import {catchError, throwError} from "rxjs";
import {Router} from "@angular/router";

export const apiKeyInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const cloned = req.clone({
    setHeaders: {
      'api-key': authService.apiKey!,
    },
  })
  return next(cloned).pipe(
    catchError(err => {
      if (err instanceof HttpErrorResponse) {
        if (err.status === 403) {
          authService.unsetKey();
          router.navigate(['/unauthorized']);
        }
      }
      return throwError(() => err);
    })
  );
};
