import {Component, input, Output, OutputEmitterRef} from '@angular/core';
import {RollerShutter} from "../../../models/devices/roller-shutter.dto";
import {CompassPipe} from "../../../core/pipes/compass.pipe";
import {AsyncPipe, DatePipe} from "@angular/common";
import {IsDateInFuturePipePipe} from "../../../core/pipes/is-date-in-future.pipe";
import {SbbFlipCard} from "@sbb-esta/lyne-angular/flip-card/flip-card";
import {SbbFlipCardSummary} from "@sbb-esta/lyne-angular/flip-card/flip-card-summary";
import {SbbFlipCardDetails} from "@sbb-esta/lyne-angular/flip-card/flip-card-details";
import {SbbButton} from "@sbb-esta/lyne-angular/button/button";

@Component({
  selector: 'app-roller-shutter-cell',
  standalone: true,
  imports: [
    CompassPipe,
    DatePipe,
    IsDateInFuturePipePipe,
    AsyncPipe,
    SbbButton,
    SbbFlipCard,
    SbbFlipCardSummary,
    SbbFlipCardDetails,
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
