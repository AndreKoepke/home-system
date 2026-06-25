import {ChangeDetectionStrategy, Component, effect, input, output} from '@angular/core';
import {MotionSensor, MotionSensorConfig} from "../../../models/devices/motion-sensor.dto";
import {FormControl, FormGroup, FormsModule, ReactiveFormsModule} from "@angular/forms";
import {SbbSelect} from "@sbb-esta/lyne-angular/select";
import {SbbTimeInput} from "@sbb-esta/lyne-angular/time-input";
import {AnimationDto} from "../../../models/animation.dto";
import {Light} from "../../../models/devices/light.dto";
import {SbbFormField} from "@sbb-esta/lyne-angular/form-field";
import {SbbOption} from "@sbb-esta/lyne-angular/option";
import {Weather} from "../../../models/devices/weather.dto";
import {WeatherValuePipe} from "../../../core/pipes/weahter-value.pipe";
import {ChronoUnit, Duration} from "@js-joda/core";
import {debounceTime, filter, map, mergeWith, Observable} from "rxjs";
import {AsyncPipe} from "@angular/common";
import {takeUntilDestroyed, toObservable} from "@angular/core/rxjs-interop";
import {SbbTab, SbbTabGroup, SbbTabLabel} from "@sbb-esta/lyne-angular/tabs";
import {SbbSelectionExpansionPanel} from "@sbb-esta/lyne-angular/selection-expansion-panel";
import {SbbCheckboxPanel} from "@sbb-esta/lyne-angular/checkbox/checkbox-panel";
import {RollerShutter} from "../../../models/devices/roller-shutter.dto";

@Component({
  selector: 'app-motion-sensor-form',
  imports: [
    FormsModule,
    ReactiveFormsModule,
    SbbFormField,
    SbbOption,
    SbbSelect,
    SbbTimeInput,
    WeatherValuePipe,
    AsyncPipe,
    SbbTabGroup,
    SbbTabLabel,
    SbbTab,
    SbbSelectionExpansionPanel,
    SbbCheckboxPanel
  ],
  templateUrl: './motion-sensor-form.html',
  standalone: true,
  styleUrl: './motion-sensor-form.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class MotionSensorForm {

  sensor = input.required<MotionSensor>()
  devices = input.required<Light[]>();
  rollerShutters = input.required<RollerShutter[]>();
  animations = input.required<AnimationDto[]>();
  weather = input.required<Weather>();
  onSave = output<MotionSensorConfig>();

  public keepMovingHumanReadable$: Observable<string | undefined>;

  public form = new FormGroup({
      name: new FormControl<string>('', {updateOn: 'blur'}),
      lights: new FormControl<string[]>([]),
      lightsAtNight: new FormControl<string[]>([]),
      keepMovingForSeconds: new FormControl<number | undefined>(undefined),
      turnLightOnWhenMovement: new FormControl<boolean>(false),
      onlyTurnOnWhenDarkerAs: new FormControl<number | undefined>(undefined),
      turnOnWhenRollerShutterIsClosed: new FormControl<string | undefined>(undefined),
      selfLightNoise: new FormControl<number | undefined>(undefined),
      notBefore: new FormControl<string | undefined>(undefined),
      animationId: new FormControl<string | undefined>(undefined),
      animationAtNightId: new FormControl<string | undefined>(undefined)
    },
    {
      updateOn: 'change'
    }
  );


  constructor() {
    effect(() => {
      this.form.patchValue({
        ...this.sensor().config,
        keepMovingForSeconds: this.sensor().config?.keepMovingFor
          ? Duration.parse(this.sensor().config!.keepMovingFor).seconds()
          : undefined
      }, {emitEvent: false});
    });

    this.keepMovingHumanReadable$ = this.form.controls.keepMovingForSeconds.valueChanges.pipe(
      map(seconds => seconds
        ? Duration.of(seconds, ChronoUnit.SECONDS)
        : undefined),
      mergeWith(toObservable(this.sensor).pipe(map(sensorUpdate => sensorUpdate.config?.keepMovingFor
        ? Duration.parse(sensorUpdate.config!.keepMovingFor)
        : undefined))),
      filter(value => value !== undefined),
      map(duration => {
        if (duration.toMinutesPart()) {
          return `${duration.toMinutesPart()}min ${duration.toSecondsPart()}s`;
        }
        return undefined;
      })
    );

    this.form.valueChanges
      .pipe(
        takeUntilDestroyed(),
        debounceTime(500)
      )
      .subscribe(
        config => this.onSave.emit({
          ...config,
          keepMovingFor: config.keepMovingForSeconds
            ? Duration.of(config.keepMovingForSeconds, ChronoUnit.SECONDS).toString()
            : undefined
        } as MotionSensorConfig));
  }

}
