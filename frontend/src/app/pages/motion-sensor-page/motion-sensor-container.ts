import {ChangeDetectionStrategy, Component, inject} from "@angular/core";
import {AsyncPipe} from "@angular/common";
import {SbbLoadingIndicatorCircle} from "@sbb-esta/lyne-angular/loading-indicator-circle";
import {MotionSensorService} from "../../services/motion-sensor.service";
import {WeatherService} from "../../services/weather.service";
import {MotionSensorPage} from "./motion-sensor-page";
import {combineLatestWith, map} from "rxjs";


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
      />
    } @else {
      <sbb-loading-indicator-circle/>
    }`,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class MotionSensorContainer {

  private motionSensorService = inject(MotionSensorService);
  private weatherService = inject(WeatherService);

  public container$ = this.motionSensorService.sensors$.pipe(
    combineLatestWith(this.weatherService.weather$),
    map(([sensors, weather]) => {
      return {sensors, weather}
    }),
  )

}
