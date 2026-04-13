import {Component, input, output} from '@angular/core';
import {MotionSensor, MotionSensorConfig} from "../../models/devices/motion-sensor.dto";
import {Weather} from "../../models/devices/weather.dto";
import {MotionSensorForm} from "./motion-sensor-form/motion-sensor-form";
import {SbbAccordion} from "@sbb-esta/lyne-angular/accordion";
import {Light} from "../../models/devices/light.dto";
import {
  SbbExpansionPanel,
  SbbExpansionPanelContent,
  SbbExpansionPanelHeader
} from "@sbb-esta/lyne-angular/expansion-panel";
import {AnimationDto} from "../../models/animation.dto";
import {SbbIcon} from "@sbb-esta/lyne-angular/icon";

@Component({
  selector: 'app-motion-sensor-page',
  imports: [
    SbbAccordion,
    SbbExpansionPanel,
    SbbExpansionPanelContent,
    SbbExpansionPanelHeader,
    MotionSensorForm,
    SbbIcon
  ],
  templateUrl: './motion-sensor-page.html',
  styleUrl: './motion-sensor-page.scss'
})
export class MotionSensorPage {


  motionSensors = input.required<Map<string, MotionSensor>>();
  currentWeather = input.required<Weather>();
  devices = input.required<Light[]>();
  animations = input.required<AnimationDto[]>();

  save = output<MotionSensorConfig>();

}
