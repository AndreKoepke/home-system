import {ChangeDetectionStrategy, Component} from "@angular/core";
import {AsyncPipe} from "@angular/common";
import {StartPageComponent} from "./start-page.component";
import {DevicesService} from "../../services/devices.service";
import {map, Observable} from "rxjs";
import {Light} from "../../models/devices/light.dto";

@Component({
  standalone: true,
  imports: [
    AsyncPipe,
    StartPageComponent
  ],
  template: `
    @if (lights$ | async; as lights) {
      <app-start-page [devices]="lights"></app-start-page>
    }`,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class StartPageContainerComponent {

  lights$: Observable<Light[]>;


  constructor(devicesService: DevicesService) {
    this.lights$ = devicesService.devices$.pipe(map(deviceMap => [...deviceMap.values()]));
  }

}
