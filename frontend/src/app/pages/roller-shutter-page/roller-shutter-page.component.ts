import {ChangeDetectionStrategy, Component, input} from '@angular/core';
import {RollerShutter} from "../../models/devices/roller-shutter.dto";
import {RollerShutterCellComponent} from "./roller-shutter-cell/roller-shutter-cell.component";

@Component({
  selector: 'app-roller-shutter-page',
  standalone: true,
  imports: [
    RollerShutterCellComponent
  ],
  templateUrl: './roller-shutter-page.component.html',
  styleUrl: './roller-shutter-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class RollerShutterPageComponent {

  public rollerShutters = input.required<RollerShutter[]>();

}
