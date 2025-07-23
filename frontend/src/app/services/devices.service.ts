import {DestroyRef, Injectable} from '@angular/core';
import {BehaviorSubject, Observable, retry, Subject} from "rxjs";
import {environment} from "../../environments/environment";
import {Light} from "../models/devices/light.dto";
import {webSocket} from "rxjs/webSocket";
import {takeUntilDestroyed} from "@angular/core/rxjs-interop";
import {MotionSensor} from "../models/devices/sensor.dto";
import {Device} from "../models/devices/device.dto";

@Injectable({
  providedIn: 'root'
})
export class DevicesService {

  private readonly sensorListener = new Listener<MotionSensor>('sensors', this.destroyRef);
  private readonly lightListener = new Listener<Light>('lights', this.destroyRef);


  constructor(private readonly destroyRef: DestroyRef) {
  }

  public get lights$(): Observable<Map<string, Light>> {
    return this.lightListener.subject$;
  }

  public get sensors$(): Observable<Map<string, MotionSensor>> {
    return this.sensorListener.subject$;
  }
}

export function baseUrl(): string {
  return `${environment.backend.host}/${environment.backend.path}secured`;
}

export class Listener<T extends Device> {

  private devices = new Map<string, T>();
  public subject$: Subject<Map<string, T>> = new BehaviorSubject(this.devices);
  private readonly websocket$ = webSocket<T>(Listener.getUrl(this.name));

  constructor(private readonly name: string, destroyRef: DestroyRef) {
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
