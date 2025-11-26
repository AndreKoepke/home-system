import {DestroyRef, Injectable} from '@angular/core';
import {HttpClient} from "@angular/common/http";
import {EMPTY, mergeMap, Observable} from "rxjs";
import {Listener} from "./devices.service";
import {RollerShutter} from "../models/devices/roller-shutter.dto";
import {getHttpBaseUrl} from "../url-resolver";

@Injectable({
  providedIn: 'root'
})
export class RollerShutterService {

  private readonly rollerShutterListener = new Listener<RollerShutter>('roller-shutters', this.destroyRef);

  constructor(private readonly destroyRef: DestroyRef,
              private readonly httpClient: HttpClient) {
  }

  public openAllRollerShutters(): Observable<never> {
    return this.httpClient.post(`${getHttpBaseUrl()}v1/devices/roller-shutters/open-all`, null)
      .pipe(mergeMap(() => EMPTY));
  }

  public closeAllRollerShutters(): Observable<never> {
    return this.httpClient.post(`${getHttpBaseUrl()}v1/devices/roller-shutters/close-all`, null)
      .pipe(mergeMap(() => EMPTY));
  }

  public calcAgain(): Observable<never> {
    return this.httpClient.post(`${getHttpBaseUrl()}v1/devices/roller-shutters/start-calculating-again`, null)
      .pipe(mergeMap(() => EMPTY));
  }

  public block(id: string): Observable<never> {
    return this.httpClient.post(`${getHttpBaseUrl()}v1/devices/roller-shutters/${id}/block`, null)
      .pipe(mergeMap(() => EMPTY));
  }

  public unblock(id: string): Observable<never> {
    return this.httpClient.post(`${getHttpBaseUrl()}v1/devices/roller-shutters/${id}/unblock`, null)
      .pipe(mergeMap(() => EMPTY));
  }

  public setLift$(id: string, lift: number): Observable<never> {
    return this.httpClient.post(`${getHttpBaseUrl()}v1/devices/roller-shutters/${id}/lift/to/${lift}`, null)
      .pipe(mergeMap(() => EMPTY));
  }

  public setTilt$(id: string, tilt: number): Observable<never> {
    return this.httpClient.post(`${getHttpBaseUrl()}v1/devices/roller-shutters/${id}/tilt/to/${tilt}`, null)
      .pipe(mergeMap(() => EMPTY));
  }

  public get rollerShutters$(): Observable<Map<string, RollerShutter>> {
    return this.rollerShutterListener.subject$;
  }
}
