import {DestroyRef, Injectable} from '@angular/core';
import {HttpClient} from "@angular/common/http";
import {EMPTY, mergeMap, Observable} from "rxjs";
import {environment} from "../../environments/environment";
import {baseUrl, Listener} from "./devices.service";
import {RollerShutter} from "../models/devices/roller-shutter.dto";

@Injectable({
  providedIn: 'root'
})
export class RollerShutterService {

  private readonly rollerShutterListener = new Listener<RollerShutter>('roller-shutters', this.destroyRef);

  constructor(private readonly destroyRef: DestroyRef,
              private readonly httpClient: HttpClient) {
  }

  public openAllRollerShutters(): Observable<never> {
    return this.httpClient.post(`${environment.backend.protocol}${baseUrl()}/v1/devices/roller-shutters/open-all`, null)
      .pipe(mergeMap(() => EMPTY));
  }

  public closeAllRollerShutters(): Observable<never> {
    return this.httpClient.post(`${environment.backend.protocol}${baseUrl()}/v1/devices/roller-shutters/close-all`, null)
      .pipe(mergeMap(() => EMPTY));
  }

  public calcAgain(): Observable<never> {
    return this.httpClient.post(`${environment.backend.protocol}${baseUrl()}/v1/devices/roller-shutters/start-calculating-again`, null)
      .pipe(mergeMap(() => EMPTY));
  }

  public block(id: string): Observable<never> {
    return this.httpClient.post(`${environment.backend.protocol}${baseUrl()}/v1/devices/roller-shutters/${id}/block`, null)
      .pipe(mergeMap(() => EMPTY));
  }

  public unblock(id: string): Observable<never> {
    return this.httpClient.post(`${environment.backend.protocol}${baseUrl()}/v1/devices/roller-shutters/${id}/unblock`, null)
      .pipe(mergeMap(() => EMPTY));
  }

  public get rollerShutters$(): Observable<Map<string, RollerShutter>> {
    return this.rollerShutterListener.subject$;
  }
}
