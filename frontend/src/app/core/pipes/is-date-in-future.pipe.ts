import {Pipe, PipeTransform} from "@angular/core";
import {map, Observable, of, timer} from "rxjs";

@Pipe({
  name: 'isDateInFuturePipe',
  pure: true,
  standalone: true
})
export class IsDateInFuturePipePipe implements PipeTransform {


  transform(value: string | undefined, updateInterval: number = 30_000): Observable<boolean> {
    if (!value) {
      return of(false);
    }

    return timer(0, updateInterval)
      .pipe(map(_ => new Date(value) > new Date()));
  }

}
