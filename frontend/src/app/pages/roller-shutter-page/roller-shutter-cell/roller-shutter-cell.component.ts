import {Component, input} from '@angular/core';
import {RollerShutter} from "../../../models/devices/roller-shutter.dto";
import {CompassPipe} from "../../../core/pipes/compass.pipe";
import {DatePipe} from "@angular/common";

@Component({
  selector: 'app-roller-shutter-cell',
  standalone: true,
  imports: [
    CompassPipe,
    DatePipe
  ],
  templateUrl: './roller-shutter-cell.component.html',
  styleUrl: './roller-shutter-cell.component.scss'
})
export class RollerShutterCellComponent {

  public rollerShutter = input.required<RollerShutter>();

}
