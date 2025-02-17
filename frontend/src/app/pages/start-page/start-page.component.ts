import {ChangeDetectionStrategy, Component, computed, input} from '@angular/core';
import {Light} from "../../models/devices/light.dto";
import {Weather} from "../../models/devices/weather.dto";

@Component({
  selector: 'app-start-page',
  standalone: true,
  templateUrl: './start-page.component.html',
  styleUrl: './start-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class StartPageComponent {

  public devices = input.required<Light[]>();
  public devicesThatAreOn = computed(() => this.devices()
    .filter(device => device.on && device.reachable)
    .sort((a, b) => a.name.localeCompare(b.name)))

  public weather = input.required<Weather>();
}
