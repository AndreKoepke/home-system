import {Component, input, output} from '@angular/core';
import {TimerConfig} from "../../models/timer-config.dto";
import {SbbAccordion} from "@sbb-esta/lyne-angular/accordion";
import {SbbExpansionPanelContent} from "@sbb-esta/lyne-angular/expansion-panel/expansion-panel-content";
import {SbbExpansionPanelHeader} from "@sbb-esta/lyne-angular/expansion-panel/expansion-panel-header";
import {SbbExpansionPanel} from "@sbb-esta/lyne-angular/expansion-panel/expansion-panel";
import {TimerFormComponent} from "./timer-form/timer-form.component";
import {Light} from "../../models/devices/light.dto";

@Component({
  selector: 'app-timer-page',
  standalone: true,
  imports: [
    SbbAccordion,
    SbbExpansionPanel,
    SbbExpansionPanelContent,
    SbbExpansionPanelHeader,
    TimerFormComponent,
  ],
  templateUrl: './timer-page.component.html',
  styleUrl: './timer-page.component.scss'
})
export class TimerPageComponent {

  public timerConfigs = input.required<Array<TimerConfig>>();
  public devices = input.required<Array<Light>>();
  public save = output<TimerConfig>();

}
