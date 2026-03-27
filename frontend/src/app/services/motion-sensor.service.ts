import {DestroyRef, Injectable} from '@angular/core';
import {Observable} from "rxjs";
import {MotionSensor} from "../models/devices/sensor.dto";
import {Listener} from "./devices.service";

@Injectable({
  providedIn: 'root'
})
export class MotionSensorService {

  private readonly sensorListener = new Listener<MotionSensor>('devices/sensors/motion-sensors',
    this.destroyRef);


  constructor(private readonly destroyRef: DestroyRef) {
  }

  public get sensors$(): Observable<Map<string, MotionSensor>> {
    return this.sensorListener.subject$;
  }
}
