import {ChangeDetectionStrategy, Component, inject, signal} from "@angular/core";
import {AsyncPipe} from "@angular/common";
import {combineLatest, map} from "rxjs";
import {TimerPageComponent} from "./timer-page.component";
import {TimerService} from "../../services/timer.service";
import {DevicesService} from "../../services/devices.service";
import {TimerConfig} from "../../models/timer-config.dto";
import {SbbLoadingIndicatorCircle} from "@sbb-esta/lyne-angular/loading-indicator-circle";

@Component({
  standalone: true,
  imports: [
    AsyncPipe,
    TimerPageComponent,
    SbbLoadingIndicatorCircle
  ],
  template: `
    @if (!isLoading()) {
      @if (viewContainer$ | async; as container) {
        <app-timer-page [timerConfigs]="container.timer"
                        [devices]="container.devices"
                        (save)="save($event)"

        />
      }
    } @else {
      <sbb-loading-indicator-circle/>
    }`,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class TimerPageComponentContainer {

  public readonly timerService = inject(TimerService);
  private readonly deviceService = inject(DevicesService);

  public isLoading = signal(false);

  public timers$ = this.timerService.getConfigs();
  public devices$ = this.deviceService.lights$
    .pipe(map(map => [...map.values()]));

  public viewContainer$ = combineLatest([this.timers$, this.devices$])
    .pipe(map(([timer, devices]) => {
      return {timer, devices};
    }));

  public save(config: TimerConfig): void {
    this.isLoading.set(true);
    this.timerService.saveConfig(config)
      .subscribe(() => {
        this.isLoading.set(false);
      })
  }
}
