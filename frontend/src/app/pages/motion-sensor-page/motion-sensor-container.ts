import {ChangeDetectionStrategy, Component, inject} from "@angular/core";
import {AsyncPipe} from "@angular/common";
import {SbbLoadingIndicatorCircle} from "@sbb-esta/lyne-angular/loading-indicator-circle";
import {MotionSensorService} from "../../services/motion-sensor.service";
import {WeatherService} from "../../services/weather.service";
import {MotionSensorPage} from "./motion-sensor-page";
import {combineLatestWith, map} from "rxjs";
import {DevicesService} from "../../services/devices.service";
import {AnimationService} from "../../services/animation.service";
import {RollerShutterService} from "../../services/roller-shutter.service";


@Component({
  standalone: true,
  imports: [
    AsyncPipe,
    SbbLoadingIndicatorCircle,
    MotionSensorPage
  ],
  template: `
    @if (container$ | async; as container) {
      <app-motion-sensor-page
        [motionSensors]="container.sensors"
        [currentWeather]="container.weather"
        [devices]="container.lights"
        [rollerShutters]="container.rollerShutters"
        [animations]="container.animations"
        (save)="motionSensorService.saveConfig($event)"
      />
    } @else {
      <sbb-loading-indicator-circle/>
    }`,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class MotionSensorContainer {

  public motionSensorService = inject(MotionSensorService);
  private weatherService = inject(WeatherService);
  private deviceService = inject(DevicesService);
  private rollerShutterService = inject(RollerShutterService);
  private animationService = inject(AnimationService);

  public container$ = this.motionSensorService.sensors$.pipe(
    combineLatestWith(this.weatherService.weather$,
      this.deviceService.lights$,
      this.animationService.getAnimations$(),
      this.rollerShutterService.rollerShutters$.pipe(map(rollerShutterMap => [...rollerShutterMap.values()]))),
    map(([sensors, weather, lights, animations, rollerShutters]) => {
      return {sensors, weather, animations, rollerShutters, lights: [...lights.values()]}
    }),
  )

}
