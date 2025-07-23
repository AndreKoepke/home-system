import {Component, input, Output, OutputEmitterRef} from '@angular/core';
import {RollerShutter} from "../../../models/devices/roller-shutter.dto";
import {CompassPipe} from "../../../core/pipes/compass.pipe";
import {AsyncPipe, DatePipe} from "@angular/common";
import {IsDateInFuturePipePipe} from "../../../core/pipes/is-date-in-future.pipe";
import {CircleButtonComponent} from "../../../components/buttons/circle-button/circle-button.component";

@Component({
  selector: 'app-roller-shutter-cell',
  standalone: true,
  imports: [
    CompassPipe,
    DatePipe,
    IsDateInFuturePipePipe,
    AsyncPipe,
    CircleButtonComponent
  ],
  templateUrl: './roller-shutter-cell.component.html',
  styleUrl: './roller-shutter-cell.component.scss'
})
export class RollerShutterCellComponent {

  public rollerShutter = input.required<RollerShutter>();

  @Output()
  public block = new OutputEmitterRef<void>();

  @Output()
  public unblock = new OutputEmitterRef<void>();

}
