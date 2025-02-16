import {DestroyRef, Injectable} from '@angular/core';
import {HttpClient} from "@angular/common/http";
import {Observable, ReplaySubject, retry} from "rxjs";
import {environment} from "../../environments/environment";
import {Light} from "../models/devices/light.dto";
import {webSocket} from "rxjs/webSocket";
import {takeUntilDestroyed} from "@angular/core/rxjs-interop";

@Injectable({
  providedIn: 'root'
})
export class DevicesService {

  private devices = new Map<string, Light>;
  private devicesSubject = new ReplaySubject<Map<string, Light>>(1);

  public get devices$() {
    return this.devicesSubject;
  }

  private webSocketSubject = webSocket<Light>({
    url: `${environment.backend.webSocketProtocol}${environment.backend.host}/${environment.backend.path}secured/ws/v1/devices`,
    openObserver: {
      next: () => this.hydrateDevices(),
      error: err => console.error('WebSocketConnection threw error', err),
      complete: () => console.warn('WebSocketConnection was closed')
    }
  });

  constructor(private httpClient: HttpClient, private destroyRef: DestroyRef) {
    this.setupWebsocketListener();
  }

  private hydrateDevices(): void {
    this.fetchLights$()
      .subscribe(lights => {
        this.devices = new Map(lights.map(light => [light.id, light]));
        this.devicesSubject.next(this.devices);
      });
  }

  private setupWebsocketListener() {
    this.webSocketSubject
      .pipe(
        retry({delay: 5000}),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe(message => {
        this.devices.set(message.id, message);
        this.devices = new Map(this.devices);
        this.devicesSubject.next(this.devices);
      });
  }

  private fetchLights$(): Observable<Light[]> {
    return this.httpClient.get<Light[]>(`${this.backendUrl}/secured/v1/devices/lights`);
  }

  private get backendUrl(): string {
    return `${environment.backend.protocol}${environment.backend.host}/${environment.backend.path}`;
  }
}
