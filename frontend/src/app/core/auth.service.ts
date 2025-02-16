import {Inject, inject, Injectable, PLATFORM_ID} from '@angular/core';
import {ActivatedRoute, CanActivateFn, GuardResult, MaybeAsync, Router} from "@angular/router";
import {isPlatformBrowser} from "@angular/common";

@Injectable({
  providedIn: 'root'
})
export class AuthService {

  public apiKey: string | undefined;

  constructor(private route: ActivatedRoute,
              @Inject(PLATFORM_ID) private platformId: Object,
              private router: Router) {
  }

  private tryToFindApiKey() {
    let fromLocalStorage = localStorage.getItem('api-key') || undefined;

    console.log(`>>>> look up api-key from localStorage`, fromLocalStorage);
    if (!this.isApiKeySet(fromLocalStorage)) {
      const apiKeyFromRoute = this.getApiKeyFromRoute();
      if (apiKeyFromRoute) {
        localStorage.setItem('api-key', apiKeyFromRoute);
        fromLocalStorage = apiKeyFromRoute;
      }
    }

    this.clearApiKeyFromRoute();
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

  public isAuthorized(): boolean {
    if (!this.isApiKeySet(this.apiKey) && isPlatformBrowser(this.platformId)) {
      this.tryToFindApiKey()
    }

    return this.isApiKeySet(this.apiKey);
  }

  private isApiKeySet(key: string | undefined): boolean {
    return key !== undefined && key !== '' && key !== 'undefined'
  }

  private getApiKeyFromRoute(): string | undefined {
    return this.route.snapshot.params['api-key'] as string | undefined;
  }

  private clearApiKeyFromRoute() {
    this.router.navigate([], {queryParams: {'api-key': null}, queryParamsHandling: 'merge'});
  }
}

export const AuthGuard: CanActivateFn = (): MaybeAsync<GuardResult> => {
  if (inject(AuthService).isAuthorized()) {
    return true;
  }

  return inject(Router).parseUrl('/unauthorized');
}
