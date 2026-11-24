import {ChangeDetectionStrategy, Component, input, output} from '@angular/core';
import {TimerConfig} from "../../models/timer-config.dto";
import {TimerFormComponent} from "./timer-form/timer-form.component";
import {Light} from "../../models/devices/light.dto";

@Component({
  selector: 'app-timer-page',
  standalone: true,
  imports: [
    TimerFormComponent,
  ],
  templateUrl: './timer-page.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './timer-page.component.scss'
})
export class TimerPageComponent {

  public timerConfigs = input.required<Array<TimerConfig>>();
  public devices = input.required<Array<Light>>();
  public save = output<TimerConfig>();

}
