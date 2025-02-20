import {DestroyRef, Injectable} from '@angular/core';
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

  public get lights$(): Observable<Map<string, Light>> {
    return this.actorSubject;
  }

  public get sensors$(): Observable<Map<string, Sensor>> {
    return this.sensorsSubject;
  }

  private websocketActorsSubject = webSocket<Light>({
    url: `${environment.backend.webSocketProtocol}${environment.backend.host}/${environment.backend.path}secured/ws/v1/devices/actors`,
  });

  private websocketSensorSubject = webSocket<Sensor>({
    url: `${environment.backend.webSocketProtocol}${environment.backend.host}/${environment.backend.path}secured/ws/v1/devices/sensors`
  });

  constructor(private destroyRef: DestroyRef) {
    this.setupWebsocketListener();
  }

  private setupWebsocketListener() {
    this.websocketActorsSubject
      .pipe(
        retry({delay: 5000}),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe(light => this.actorUpdate(light));

    this.websocketSensorSubject
      .pipe(
        retry({delay: 5000}),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe(sensor => this.sensorUpdate(sensor));
  }

  private sensorUpdate(message: Sensor): void {
    this.sensors.set(message.id, message);
    this.sensors = new Map(this.sensors);
    this.sensorsSubject.next(this.sensors);
  }

  private actorUpdate(message: Light): void {
    this.actors.set(message.id, message);
    this.actors = new Map(this.actors);
    this.actorSubject.next(this.actors);
  }
}
