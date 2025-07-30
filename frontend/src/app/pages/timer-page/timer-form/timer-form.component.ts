import {ChangeDetectionStrategy, Component, effect, inject, input, LOCALE_ID, output} from '@angular/core';
import {SbbFormField} from "@sbb-esta/lyne-angular/form-field/form-field";
import {SbbTimeInput} from "@sbb-esta/lyne-angular/time-input";
import {Light} from "../../../models/devices/light.dto";
import {SbbOption} from "@sbb-esta/lyne-angular/option/option";
import {SbbSelect} from "@sbb-esta/lyne-angular/select";
import {FormControl, FormGroup, ReactiveFormsModule} from "@angular/forms";
import {TimerConfig} from "../../../models/timer-config.dto";
import {SbbButton} from "@sbb-esta/lyne-angular/button/button";
import {SbbActionGroup} from "@sbb-esta/lyne-angular/action-group";
import {DatePipe} from "@angular/common";
import {v4 as uuidV4} from 'uuid';
import {SbbMiniButton} from "@sbb-esta/lyne-angular/button/mini-button";

@Component({
  selector: 'app-timer-form',
  standalone: true,
  imports: [
    SbbFormField,
    SbbButton,
    SbbTimeInput,
    SbbOption,
    SbbSelect,
    ReactiveFormsModule,
    SbbActionGroup,
    SbbMiniButton
  ],
  templateUrl: './timer-form.component.html',
  styleUrl: './timer-form.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class TimerFormComponent {

  private datePipe = new DatePipe(inject(LOCALE_ID));

  public config = input<TimerConfig | undefined>();
  public devices = input<Array<Light>>();
  public onSave = output<TimerConfig>();
  public onDelete = output<TimerConfig>();

  public form = new FormGroup({
      name: new FormControl<string>('', {updateOn: 'blur'}),
      turnOnAt: new FormControl<string | undefined>(undefined),
      turnOffAt: new FormControl<string | undefined>(undefined),
      lights: new FormControl<string[]>([])
    },
    {
      updateOn: 'change'
    }
  )

  constructor() {
    effect(() => {
      const setConfig = this.config();
      if (setConfig !== undefined) {
        this.form.patchValue(setConfig, {emitEvent: false});
      }
    });
  }

  public onSubmit(): void {
    const turnOnAt = this.formatDate(this.form.value.turnOnAt);
    const turnOffAt = this.formatDate(this.form.value.turnOffAt);

    this.onSave.emit(<TimerConfig>{
      ...this.form.value,
      turnOnAt,
      turnOffAt,
      id: this.config()?.id ?? uuidV4()
    });
  }

  private formatDate(date: string | undefined | null): string | undefined {
    console.log(`>>>> test`,);
    console.log(`>>>> and so?`,); 
    if (date === undefined || date === null) {
      return undefined;
    }

    return this.datePipe.transform(new Date(date), 'HH:mm')!;
  }
}
