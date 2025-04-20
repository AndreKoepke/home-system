import {ChangeDetectionStrategy, Component} from "@angular/core";
import {AsyncPipe} from "@angular/common";
import {map, Observable} from "rxjs";
import {RollerShutter} from "../../models/devices/roller-shutter.dto";
import {RollerShutterPageComponent} from "./roller-shutter-page.component";
import {RollerShutterService} from "../../services/roller-shutter.service";

@Component({
  standalone: true,
  imports: [
    AsyncPipe,
    RollerShutterPageComponent
  ],
  template: `
    @if (rollerShutters$ | async; as rollerShutters) {
      <app-roller-shutter-page [rollerShutters]="rollerShutters"
                               (closeAll)="rollerShutterService.closeAllRollerShutters().subscribe()"
                               (openAll)="rollerShutterService.openAllRollerShutters().subscribe()"
                               (calcAgain)="rollerShutterService.calcAgain().subscribe()"
                               (block)="rollerShutterService.block($event).subscribe()"
                               (unblock)="rollerShutterService.unblock($event).subscribe()"
      />
    }`,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class RollerShutterPageContainerComponent {

  rollerShutters$: Observable<RollerShutter[]>;

  constructor(public rollerShutterService: RollerShutterService) {
    this.rollerShutters$ = rollerShutterService.rollerShutters$.pipe(map(deviceMap => [...deviceMap.values()]));
  }
}
