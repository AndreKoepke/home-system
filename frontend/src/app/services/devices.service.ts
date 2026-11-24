import {DestroyRef, inject, Injectable} from '@angular/core';
import {BehaviorSubject, filter, Observable, retry, Subject, tap} from "rxjs";
import {Light} from "../models/devices/light.dto";
import {webSocket} from "rxjs/webSocket";
import {takeUntilDestroyed} from "@angular/core/rxjs-interop";
import {Device} from "../models/devices/device.dto";
import {getWebsocketBaseUrl} from "../url-resolver";
import {AuthService} from "../core/auth.service";

@Injectable({
  providedIn: 'root'
})
export class DevicesService {

  private readonly lightListener = new Listener<Light>('devices/lights', this.destroyRef);


  constructor(private readonly destroyRef: DestroyRef) {
  }

  public get lights$(): Observable<Array<Light>> {
    return this.lightListener.subject$;
  }
}

export class Listener<T extends Device> {

  private readonly authService = inject(AuthService);

  private devicesMap = new Map<string, T>();
  private devices: T[] = [];

  private readonly websocket$ = webSocket<T | ControlMessage>({
    url: `${getWebsocketBaseUrl()}${this.path}`,
    openObserver: {
      next: () => {
        this.websocket$.next({type: 'auth', token: this.authService.apiKey!});
      }
    }
  });

  public subject$: Subject<Array<T>> = new BehaviorSubject(this.devices);

  constructor(private readonly path: string, destroyRef: DestroyRef) {
    this.websocket$
      .pipe(
        tap({
          error: err => console.error('error with socket', err)
        }),
        retry({delay: 5000}),
        takeUntilDestroyed(destroyRef),
        filter(message => isNotControlMessage(message))
      )
      .subscribe(device => this.deviceUpdate(device));
  }

  private deviceUpdate(message: T): void {
    if (this.devicesMap.has(message.id)) {
      this.devicesMap.set(message.id, message);
      this.devices = [...this.devices];
    } else {
      this.devicesMap.set(message.id, message);
      this.devices = [...this.devicesMap.values()].toSorted((a, b) => a.name.localeCompare(b.name));
    }

    this.subject$.next(this.devices);
  }


}

export function isNotControlMessage<T>(message: T | ControlMessage): message is T {
  return (message as ControlMessage).type === undefined;
}

export interface ControlMessage {
  token?: string;
  type: 'auth' | 'ping' | 'pong';
}
