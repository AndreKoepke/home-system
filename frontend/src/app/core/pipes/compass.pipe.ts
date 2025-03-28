import {Pipe, PipeTransform} from '@angular/core';

@Pipe({
  name: 'compass',
  standalone: true
})
export class CompassPipe implements PipeTransform {
  transform(value: string[]): string {
    return value
      .map(directions => directions
        .split('_')
        .map(direction => direction.at(0))
        .join(''))
      .join(', ');
  }
}
