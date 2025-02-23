import {DestroyRef, Injectable} from '@angular/core';
import {BehaviorSubject, EMPTY, mergeMap, Observable, retry, Subject} from "rxjs";
import {environment} from "../../environments/environment";
import {Light} from "../models/devices/light.dto";
import {webSocket} from "rxjs/webSocket";
import {takeUntilDestroyed} from "@angular/core/rxjs-interop";
import {MotionSensor} from "../models/devices/sensor.dto";
import {Device} from "../models/devices/device.dto";
import {RollerShutter} from "../models/devices/roller-shutter.dto";
import {HttpClient} from "@angular/common/http";

@Injectable({
  providedIn: 'root'
})
export class DevicesService {

  private sensorListener = new Listener<MotionSensor>('sensors', this.destroyRef);
  private lightListener = new Listener<Light>('lights', this.destroyRef);
  private rollerShutterListener = new Listener<RollerShutter>('roller-shutters', this.destroyRef);

  constructor(private destroyRef: DestroyRef, private httpClient: HttpClient) {
  }

  public get lights$(): Observable<Map<string, Light>> {
    return this.lightListener.subject$;
  }

  public get sensors$(): Observable<Map<string, MotionSensor>> {
    return this.sensorListener.subject$;
  }

  public get rollerShutters$(): Observable<Map<string, RollerShutter>> {
    return this.rollerShutterListener.subject$;
  }

  public openAllRollerShutters(): Observable<never> {
    return this.httpClient.post(`${baseUrl()}/v1/devices/roller-shutters/open-all`, null)
      .pipe(mergeMap(() => EMPTY));
  }

  public closeAllRollerShutters(): Observable<never> {
    return this.httpClient.post(`${environment.backend.protocol}${baseUrl()}/v1/devices/roller-shutters/close-all`, null)
      .pipe(mergeMap(() => EMPTY));
  }
}

export function baseUrl(): string {
  return `${environment.backend.host}/${environment.backend.path}secured`;
}

class Listener<T extends Device> {

  private devices = new Map<string, T>();
  public subject$: Subject<Map<string, T>> = new BehaviorSubject(this.devices);
  private websocket$ = webSocket<T>(Listener.getUrl(this.name));

  constructor(private name: string, destroyRef: DestroyRef) {
    this.websocket$
      .pipe(
        retry({delay: 5000}),
        takeUntilDestroyed(destroyRef)
      )
      .subscribe(device => this.deviceUpdate(device));
  }

  public static getUrl(name: string): string {
    return `${environment.backend.webSocketProtocol}${baseUrl()}/ws/v1/devices/${name}`;
  }

  private deviceUpdate(message: T): void {
    this.devices.set(message.id, message);
    this.devices = new Map(this.devices);
    this.subject$.next(this.devices);
  }
}
