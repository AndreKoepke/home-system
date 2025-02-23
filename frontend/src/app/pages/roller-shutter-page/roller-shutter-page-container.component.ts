import {ChangeDetectionStrategy, Component} from "@angular/core";
import {AsyncPipe} from "@angular/common";
import {DevicesService} from "../../services/devices.service";
import {map, Observable} from "rxjs";
import {RollerShutter} from "../../models/devices/roller-shutter.dto";
import {RollerShutterPageComponent} from "./roller-shutter-page.component";

@Component({
  standalone: true,
  imports: [
    AsyncPipe,
    RollerShutterPageComponent
  ],
  template: `
    @if (rollerShutters$ | async; as rollerShutters) {
      <app-roller-shutter-page [rollerShutters]="rollerShutters"
                               (closeAll)="devicesService.closeAllRollerShutters().subscribe()"
                               (openAll)="devicesService.openAllRollerShutters().subscribe()"/>
    }`,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class RollerShutterPageContainerComponent {

  rollerShutters$: Observable<RollerShutter[]>;

  constructor(public devicesService: DevicesService) {
    this.rollerShutters$ = devicesService.rollerShutters$.pipe(map(deviceMap => [...deviceMap.values()]));
  }
}
