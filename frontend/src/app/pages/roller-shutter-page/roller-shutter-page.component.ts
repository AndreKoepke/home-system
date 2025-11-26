import {ChangeDetectionStrategy, Component, input, output} from '@angular/core';
import {RollerShutter} from "../../models/devices/roller-shutter.dto";
import {RollerShutterCellComponent} from "./roller-shutter-cell/roller-shutter-cell.component";
import {SbbActionGroup} from "@sbb-esta/lyne-angular/action-group";
import {SbbButton} from "@sbb-esta/lyne-angular/button/button";

@Component({
  selector: 'app-roller-shutter-page',
  standalone: true,
  imports: [
    RollerShutterCellComponent,
    SbbActionGroup,
    SbbButton
  ],
  templateUrl: './roller-shutter-page.component.html',
  styleUrl: './roller-shutter-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class RollerShutterPageComponent {

  public rollerShutters = input.required<RollerShutter[]>();

  public openAll = output();
  public closeAll = output();
  public calcAgain = output();
  public block = output<string>();
  public unblock = output<string>();
  public lift = output<{ rollerShutterId: string, lift: number }>();
  public tilt = output<{ rollerShutterId: string, tilt: number }>();
}
