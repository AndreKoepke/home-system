import {ChangeDetectionStrategy, Component, effect, input, output} from '@angular/core';
import {SbbFormField} from "@sbb-esta/lyne-angular/form-field/form-field";
import {SbbTimeInput} from "@sbb-esta/lyne-angular/time-input";
import {Light} from "../../../models/devices/light.dto";
import {SbbOption} from "@sbb-esta/lyne-angular/option/option";
import {SbbSelect} from "@sbb-esta/lyne-angular/select";
import {FormControl, FormGroup, ReactiveFormsModule} from "@angular/forms";
import {TimerConfig} from "../../../models/timer-config.dto";
import {SbbButton} from "@sbb-esta/lyne-angular/button/button";
import {SbbActionGroup} from "@sbb-esta/lyne-angular/action-group";

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
  ],
  templateUrl: './timer-form.component.html',
  styleUrl: './timer-form.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class TimerFormComponent {

  public config = input<TimerConfig | undefined>();
  public devices = input<Array<Light>>();
  public onSave = output<TimerConfig>();

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
        console.log(`>>>> setConfig`, setConfig);
        this.form.patchValue(setConfig, {emitEvent: false});
      }
    });


  }

  public onSubmit(): void {
    console.log(`>>>> submit`, this.form.value);
    this.onSave.emit(<TimerConfig>{
      ...this.form.value,
      id: this.config()?.id
    });
  }
}
