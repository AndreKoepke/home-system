import {Component, ElementRef, ViewChild} from '@angular/core';
import {interval} from "rxjs";
import {takeUntilDestroyed} from "@angular/core/rxjs-interop";

@Component({
  selector: 'app-weather-page',
  standalone: true,
  imports: [],
  templateUrl: './weather-page.component.html',
  styleUrl: './weather-page.component.scss'
})
export class WeatherPageComponent {

  @ViewChild('container')
  private iframe: ElementRef | undefined;

  public startScrolling(): void {
    let current = 0;
    let toUpper = true;
    interval(5000)
      .pipe(takeUntilDestroyed())
      .subscribe(() => {
        const maxHeight = this.iframe?.nativeElement.scrollHeight;


        if ((current + maxHeight * 0.3) > maxHeight) {
          toUpper = false;
        }

        if ((current - maxHeight * 0.3) <= 0) {
          toUpper = true;
        }

        if (toUpper) {
          current += maxHeight * 0.3;
        } else {
          current -= maxHeight * 0.3;
        }

        this.iframe?.nativeElement.scrollTo(0, current);
        console.log(current)
      });
  }

}
