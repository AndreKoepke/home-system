import {ChangeDetectionStrategy, Component, input, Output, OutputEmitterRef} from '@angular/core';
import {RollerShutter} from "../../models/devices/roller-shutter.dto";
import {RollerShutterCellComponent} from "./roller-shutter-cell/roller-shutter-cell.component";
import {CircleButtonComponent} from "../../components/buttons/circle-button/circle-button.component";

@Component({
  selector: 'app-roller-shutter-page',
  standalone: true,
  imports: [
    RollerShutterCellComponent,
    CircleButtonComponent
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
