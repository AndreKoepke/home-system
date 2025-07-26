import {Component, input, output} from '@angular/core';
import {TimerConfig} from "../../models/timer-config.dto";

@Component({
  selector: 'app-timer-page',
  standalone: true,
  imports: [],
  templateUrl: './timer-page.component.html',
  styleUrl: './timer-page.component.scss'
})
export class TimerPageComponent {

  public timerConfigs = input.required<TimerConfig[]>();
  public saveTimerConfig = output<TimerConfig>()

}
