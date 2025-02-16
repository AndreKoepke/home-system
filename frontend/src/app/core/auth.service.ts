import {Inject, inject, Injectable, PLATFORM_ID} from '@angular/core';
import {ActivatedRoute, CanActivateFn, GuardResult, MaybeAsync, Router} from "@angular/router";
import {isPlatformBrowser} from "@angular/common";

@Injectable({
  providedIn: 'root'
})
export class AuthService {

  public apiKey: string | undefined;

  constructor(route: ActivatedRoute, @Inject(PLATFORM_ID) platformId: Object) {
    if (isPlatformBrowser(platformId)) {

      let fromLocalStorage = localStorage.getItem('api-key') || undefined;

      console.log(`>>>> look up api-key from localStorage`, fromLocalStorage);

      if (fromLocalStorage === undefined || fromLocalStorage === '' || fromLocalStorage === 'undefined') {
        const newKey = route.snapshot.params['api-key'];
        localStorage.setItem('api-key', newKey);
        fromLocalStorage = newKey;
      }

      this.apiKey = fromLocalStorage!;
    }
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
    return this.apiKey !== undefined;
  }
}

export const AuthGuard: CanActivateFn = (): MaybeAsync<GuardResult> => {
  if (inject(AuthService).isAuthorized()) {
    return true;
  }

  return inject(Router).parseUrl('/unauthorized');
}
