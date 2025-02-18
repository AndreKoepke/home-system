import {Inject, inject, Injectable, PLATFORM_ID} from '@angular/core';
import {ActivatedRouteSnapshot, CanActivateFn, GuardResult, MaybeAsync, Router} from "@angular/router";
import {isPlatformBrowser} from "@angular/common";

@Injectable({
  providedIn: 'root'
})
export class AuthService {

  public apiKey: string | undefined;

  constructor(@Inject(PLATFORM_ID) private platformId: Object,
              private router: Router) {
  }

  private tryToFindApiKey(route: ActivatedRouteSnapshot) {
    let fromLocalStorage = localStorage.getItem('api-key') || undefined;
    const apiKeyFromRoute = this.getApiKeyFromRoute(route);
    if (apiKeyFromRoute) {
      this.clearApiKeyFromRoute();
      localStorage.setItem('api-key', apiKeyFromRoute);
      fromLocalStorage = apiKeyFromRoute;
    }

    this.apiKey = fromLocalStorage!;
  }

  public setKey(newKey: string): void {
    localStorage.setItem('api-key', newKey);
    this.apiKey = newKey;
  }

  public unsetKey(): void {
    localStorage.removeItem('api-key');
    this.apiKey = undefined;
  }

  public isAuthorized(route: ActivatedRouteSnapshot): boolean {
    if (!this.isApiKeySet(this.apiKey) && isPlatformBrowser(this.platformId)) {
      this.tryToFindApiKey(route);
    }

    return this.isApiKeySet(this.apiKey);
  }

  private isApiKeySet(key: string | undefined): boolean {
    return key !== undefined && key !== '' && key !== 'undefined'
  }

  private getApiKeyFromRoute(route: ActivatedRouteSnapshot): string | undefined {
    return route.queryParams['api-key'] as string | undefined;
  }

  private clearApiKeyFromRoute() {
    this.router.navigate([], {
      queryParams: {'api-key': null},
      queryParamsHandling: 'merge',
    });
  }
}

export const AuthGuard: CanActivateFn = (route: ActivatedRouteSnapshot): MaybeAsync<GuardResult> => {
  if (inject(AuthService).isAuthorized(route)) {
    return true;
  }

  return inject(Router).parseUrl('/unauthorized');
}
