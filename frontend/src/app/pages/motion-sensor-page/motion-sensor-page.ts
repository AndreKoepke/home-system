import {Component, input, output} from '@angular/core';
import {MotionSensor} from "../../models/devices/sensor.dto";
import {Weather} from "../../models/devices/weather.dto";
import {MotionSensorForm} from "./motion-sensor-form/motion-sensor-form";
import {SbbAccordion} from "@sbb-esta/lyne-angular/accordion";
import {SbbExpansionPanel} from "@sbb-esta/lyne-angular/expansion-panel/expansion-panel";
import {SbbExpansionPanelContent} from "@sbb-esta/lyne-angular/expansion-panel/expansion-panel-content";
import {SbbExpansionPanelHeader} from "@sbb-esta/lyne-angular/expansion-panel/expansion-panel-header";

@Component({
  selector: 'app-motion-sensor-page',
  imports: [
    SbbAccordion,
    SbbExpansionPanel,
    SbbExpansionPanelContent,
    SbbExpansionPanelHeader,
    MotionSensorForm
  ],
  templateUrl: './motion-sensor-page.html',
  styleUrl: './motion-sensor-page.scss'
})
export class MotionSensorPage {


  motionSensors = input.required<Map<string, MotionSensor>>();
  currentWeather = input.required<Weather>();

  save = output<MotionSensor>();

}
