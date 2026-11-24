import {ApplicationConfig} from '@angular/core';
import {provideRouter} from '@angular/router';

import {routes} from './pages/routes';
import {provideClientHydration, withNoIncrementalHydration} from '@angular/platform-browser';
import {provideHttpClient, withInterceptors, withXhr} from "@angular/common/http";
import {apiKeyInterceptor} from "./core/api-key.interceptor";

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),
    provideClientHydration(withNoIncrementalHydration()),
    provideHttpClient(withXhr(), withInterceptors([
      apiKeyInterceptor
    ]))
  ]
};
