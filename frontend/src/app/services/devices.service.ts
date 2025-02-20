import {DestroyRef, Injectable} from '@angular/core';
import {HttpClient} from "@angular/common/http";
import {Observable, ReplaySubject, retry} from "rxjs";
import {environment} from "../../environments/environment";
import {Light} from "../models/devices/light.dto";
import {webSocket} from "rxjs/webSocket";
import {takeUntilDestroyed} from "@angular/core/rxjs-interop";
import {Sensor} from "../models/devices/sensor.dto";

@Injectable({
  providedIn: 'root'
})
export class DevicesService {

  private actors = new Map<string, Light>;
  private sensors = new Map<string, Sensor>;
  private actorSubject = new ReplaySubject<Map<string, Light>>(1);
  private sensorsSubject = new ReplaySubject<Map<string, Sensor>>(1);

  public get lights$() {
    return this.actorSubject;
  }

  public get sensors$() {
    return this.sensorsSubject;
  }

  private websocketActorsSubject = webSocket<Light>({
    url: `${environment.backend.webSocketProtocol}${environment.backend.host}/${environment.backend.path}secured/ws/v1/devices/lights`,
    openObserver: {
      next: () => this.hydrateDevices(),
      error: err => console.error('WebSocketConnection threw error', err),
      complete: () => console.warn('WebSocketConnection was closed')
    }
  });

  private websocketSensorSubject = webSocket<Sensor>({
    url: `${environment.backend.webSocketProtocol}${environment.backend.host}/${environment.backend.path}secured/ws/v1/devices/sensors`,
    openObserver: {
      next: () => {
      },
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
        this.actors = new Map(lights.map(light => [light.id, light]));
        this.actorSubject.next(this.actors);
      });
  }

  private setupWebsocketListener() {
    this.websocketActorsSubject
      .pipe(
        retry({delay: 5000}),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe(message => {
        this.actors.set(message.id, message);
        this.actors = new Map(this.actors);
        this.actorSubject.next(this.actors);
      });

    this.websocketSensorSubject
      .pipe(
        retry({delay: 5000}),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe(message => {
        this.sensors.set(message.id, message);
        this.sensors = new Map(this.actors);
        this.sensorsSubject.next(this.actors);
      });
  }

  private fetchLights$(): Observable<Light[]> {
    return this.httpClient.get<Light[]>(`${this.backendUrl}/secured/v1/devices/lights`);
  }

  private get backendUrl(): string {
    return `${environment.backend.protocol}${environment.backend.host}/${environment.backend.path}`;
  }
}
