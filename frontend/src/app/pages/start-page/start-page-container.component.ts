import {ChangeDetectionStrategy, Component} from "@angular/core";
import {AsyncPipe} from "@angular/common";
import {StartPageComponent} from "./start-page.component";
import {DevicesService} from "../../services/devices.service";
import {map, Observable} from "rxjs";
import {Light} from "../../models/devices/light.dto";
import {WeatherService} from "../../services/weather.service";
import {Sensor} from "../../models/devices/sensor.dto";

@Component({
  standalone: true,
  imports: [
    AsyncPipe,
    StartPageComponent
  ],
  template: `
    @if (weatherService.weather$ | async; as weather) {
      @if (lights$ | async; as lights) {
        @if (sensors$ | async; as sensors) {
          <app-start-page
            [actors]="lights"
            [sensors]="sensors"
            [weather]="weather"/>
        }
      }
    }`,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class StartPageContainerComponent {

  lights$: Observable<Light[]>;
  sensors$: Observable<Sensor[]>;

  constructor(devicesService: DevicesService, public weatherService: WeatherService) {
    this.lights$ = devicesService.lights$.pipe(map(deviceMap => [...deviceMap.values()]));
    this.sensors$ = devicesService.sensors$.pipe(map(sensorMap => [...sensorMap.values()]));
  }

}
