import {Component, input, output} from '@angular/core';
import {RollerShutter} from "../../../models/devices/roller-shutter.dto";
import {CompassPipe} from "../../../core/pipes/compass.pipe";
import {AsyncPipe, DatePipe} from "@angular/common";
import {IsDateInFuturePipePipe} from "../../../core/pipes/is-date-in-future.pipe";
import {SbbButton} from "@sbb-esta/lyne-angular/button/button";
import {SbbSlider} from "@sbb-esta/lyne-angular/slider";
import {debounceTime, ReplaySubject} from "rxjs";
import {takeUntilDestroyed} from "@angular/core/rxjs-interop";
import {SbbIcon} from "@sbb-esta/lyne-angular/icon";
import {SbbFlipCard, SbbFlipCardDetails, SbbFlipCardSummary} from "@sbb-esta/lyne-angular/flip-card";
import {SbbFormField} from "@sbb-esta/lyne-angular/form-field";

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
    SbbFormField,
    SbbSlider,
    SbbIcon,
  ],
  templateUrl: './roller-shutter-cell.component.html',
  styleUrl: './roller-shutter-cell.component.scss'
})
export class RollerShutterCellComponent {

  public rollerShutter = input.required<RollerShutter>();

  public block = output();
  public unblock = output();
  public liftUpdate = output<number>();
  public tiltUpdate = output<number>();

  private lift$ = new ReplaySubject<number>(1);
  private tilt$ = new ReplaySubject<number>(1);

  constructor() {
    this.lift$.pipe(
      takeUntilDestroyed(),
      debounceTime(300)
    ).subscribe(lift => this.liftUpdate.emit(lift));

    this.tilt$.pipe(
      takeUntilDestroyed(),
      debounceTime(300)
    ).subscribe(tilt => this.tiltUpdate.emit(tilt));
  }

  public updateLift(input: InputEvent): void {
    const target = input.target as any;
    this.lift$.next(target._value);
  }

  public updateTilt(input: InputEvent): void {
    const target = input.target as any;
    this.tilt$.next(target._value);
  }
}
