import {Pipe, PipeTransform} from '@angular/core';
import {ValueAndUnit} from "../../models/devices/weather.dto";

@Pipe({
  name: 'weatherValue',
  standalone: true
})
export class WeatherValuePipe implements PipeTransform {

  transform(value: ValueAndUnit | undefined): string | undefined {
    if (value === undefined) {
      return undefined;
    }

    return `${value.value} ${value.unit}`;
  }
}
