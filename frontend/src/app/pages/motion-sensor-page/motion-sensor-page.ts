import {ChangeDetectionStrategy, Component, input, output} from '@angular/core';
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
import {RollerShutter} from "../../models/devices/roller-shutter.dto";

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
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './motion-sensor-page.scss'
})
export class MotionSensorPage {

  motionSensors = input.required<{
    configured: MotionSensor[];
    unconfigured: MotionSensor[]
  }, Map<string, MotionSensor>>({
    transform: (sensors) => {

      const configured = new Array<MotionSensor>();
      const unconfigured = new Array<MotionSensor>();

      [...sensors.values()]
        .forEach(sensor => sensor.config === undefined
          ? unconfigured.push(sensor)
          : configured.push(sensor))

      return {
        configured: configured.toSorted(this.sortMotionSensors),
        unconfigured: unconfigured.toSorted(this.sortMotionSensors),
      };
    }
  });
  currentWeather = input.required<Weather>();
  devices = input.required<Light[]>();
  animations = input.required<AnimationDto[]>();
  rollerShutters = input.required<RollerShutter[]>();

  save = output<MotionSensorConfig>();


  private sortMotionSensors(a: MotionSensor, b: MotionSensor): number {
    return a.name.localeCompare(b.name);
  }
}
