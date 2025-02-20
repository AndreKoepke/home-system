import {Pipe, PipeTransform} from "@angular/core";
import {EMPTY, map, Observable, timer} from "rxjs";

@Pipe({
  name: 'dateAgo',
  pure: true,
  standalone: true
})
export class DateAgoPipe implements PipeTransform {

  readonly intervals: Array<{ name: string; value: number }> = [
    {name: 'Jahren', value: 31536000},
    {name: 'Monaten', value: 2592000},
    {name: 'Wochen', value: 604800},
    {name: 'Tagen', value: 86400},
    {name: 'Stunden', value: 3600},
    {name: 'Minuten', value: 60},
    {name: 'Sekunden', value: 1},
  ];

  transform(value: string | undefined, updateInterval: number = 30_000): Observable<string> {

    if (!value) {
      return EMPTY;
    }

    return timer(0, updateInterval)
      .pipe(map(_ => {
          const seconds = Math.round((+new Date() - +new Date(value)) / 1000);
          if (seconds < 29) {
            return 'Grade eben';
          }

          let counter: number;
          for (const intervall of this.intervals) {
            counter = Math.round(seconds / intervall.value);
            if (counter > 0) {
              return `${counter} ${intervall.name}`;
            }
          }

          return '';
        }
      ));
  }

}
