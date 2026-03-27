import {Component, input, output} from '@angular/core';
import {MotionSensor} from "../../../models/devices/sensor.dto";

@Component({
  selector: 'app-motion-sensor-form',
  imports: [],
  templateUrl: './motion-sensor-form.html',
  styleUrl: './motion-sensor-form.scss'
})
export class MotionSensorForm {

  sensor = input.required<MotionSensor>()

  onSave = output<MotionSensor>();

}
