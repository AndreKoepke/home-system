import {ChangeDetectionStrategy, Component, computed, input} from '@angular/core';
import {Light} from "../../models/devices/light.dto";
import {Weather} from "../../models/devices/weather.dto";
import {MotionSensor} from "../../models/devices/sensor.dto";
import {SbbTeaserProduct} from "@sbb-esta/lyne-angular/teaser-product/teaser-product";
import {SbbImage} from "@sbb-esta/lyne-angular/image";
import {SbbTitle} from "@sbb-esta/lyne-angular/title";
import {WeatherValuePipe} from "../../core/pipes/weahter-value.pipe";
import {DateAgoPipe} from "../../core/pipes/date-ago.pipe";
import {AsyncPipe} from "@angular/common";

@Component({
  selector: 'app-start-page',
  standalone: true,
  templateUrl: './start-page.component.html',
  styleUrl: './start-page.component.scss',
  imports: [
    SbbTeaserProduct, SbbImage, SbbTitle, WeatherValuePipe, DateAgoPipe, AsyncPipe
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StartPageComponent {

  public actors = input.required<Light[]>();
  public lightsThatAreOn = computed(() => this.actors()
    .filter(device => device.on && device.reachable)
    .sort((a, b) => a.name.localeCompare(b.name)))

  public sensors = input.required<MotionSensor[]>();
  public sensorsThatAreMoving = computed(() => this.sensors()
    .filter(sensor => sensor.reachable && sensor.presence)
    .sort((a, b) => a.name.localeCompare(b.name)));

  public weather = input.required<Weather>();
}
