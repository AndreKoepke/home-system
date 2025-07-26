import {ChangeDetectionStrategy, Component} from "@angular/core";
import {AsyncPipe} from "@angular/common";
import {Observable} from "rxjs";
import {TimerPageComponent} from "./timer-page.component";
import {TimerService} from "../../services/timer.service";
import {TimerConfig} from "../../models/timer-config.dto";

@Component({
  standalone: true,
  imports: [
    AsyncPipe,
    TimerPageComponent
  ],
  template: `
    @if (timers$ | async; as timers) {
      <app-timer-page [timerConfigs]="timers"
                      (saveTimerConfig)="timerService.saveConfig($event)"/>
    }`,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class TimerPageComponentContainer {

  timers$: Observable<TimerConfig[]>;

  constructor(public timerService: TimerService) {
    this.timers$ = timerService.getConfigs();
  }
}
