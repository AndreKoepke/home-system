import {ChangeDetectionStrategy, Component, input, Output, OutputEmitterRef} from '@angular/core';
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

  @Output()
  public openAll = new OutputEmitterRef<void>();

  @Output()
  public closeAll = new OutputEmitterRef<void>();

  @Output()
  public calcAgain = new OutputEmitterRef<void>();

  @Output()
  public block = new OutputEmitterRef<string>();

  @Output()
  public unblock = new OutputEmitterRef<string>();

}
