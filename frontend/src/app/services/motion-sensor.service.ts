import {DestroyRef, inject, Injectable} from '@angular/core';
import {EMPTY, mergeMap, Observable} from "rxjs";
import {MotionSensor, MotionSensorConfig} from "../models/devices/motion-sensor.dto";
import {Listener} from "./devices.service";
import {HttpClient} from "@angular/common/http";
import {getHttpBaseUrl} from "../url-resolver";

@Injectable({
  providedIn: 'root'
})
export class MotionSensorService {

  private readonly http = inject(HttpClient);

  private readonly sensorListener = new Listener<MotionSensor>('devices/sensors/motion-sensors',
    this.destroyRef);


  constructor(private readonly destroyRef: DestroyRef) {
  }

  public get sensors$(): Observable<Map<string, MotionSensor>> {
    return this.sensorListener.subject$;
  }

  public saveConfig(config: MotionSensorConfig): void {
    this.http.post(`${getHttpBaseUrl()}v1/devices/sensors/motion-sensors`, config)
      .pipe(mergeMap(() => EMPTY))
      .subscribe();
  }
}
