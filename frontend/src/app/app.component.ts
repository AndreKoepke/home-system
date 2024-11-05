import {Component, HostListener, Signal} from '@angular/core';
import {NavigationEnd, Router, RouterOutlet} from '@angular/router';
import {toSignal} from "@angular/core/rxjs-interop";
import {filter, map} from "rxjs";

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss'
})
export class AppComponent {
  title = 'home-system';

  public activeRoute: Signal<string | undefined>;


  public constructor(router: Router) {
    this.activeRoute = toSignal(router.events.pipe(
      filter(event => event instanceof NavigationEnd),
      map(event => (event as NavigationEnd).url),
    ));
  }

  private swipeCoord: [number, number] = [0, 0];
  private swipeTime: number = 0;

  @HostListener('touchstart', ['$event'])
  private handleTouchStart(event: TouchEvent): void {
    this.swipe(event, 'start');
  }


  @HostListener('touchend', ['$event'])
  private handleTouchEnd(event: TouchEvent): void {
    this.swipe(event, 'end');
  }


  swipe(e: TouchEvent, when: string): void {
    const coord: [number, number] = [e.changedTouches[0].clientX, e.changedTouches[0].clientY];
    const time = new Date().getTime();

    if (when === 'start') {
      this.swipeCoord = coord;
      this.swipeTime = time;
    } else if (when === 'end') {
      const direction = [coord[0] - this.swipeCoord[0], coord[1] - this.swipeCoord[1]];
      const duration = time - this.swipeTime;

      if (duration < 1000 //
        && Math.abs(direction[0]) > 30 // Long enough
        && Math.abs(direction[0]) > Math.abs(direction[1] * 3)) { // Horizontal enough
        const swipe = direction[0] < 0 ? 'next' : 'previous';
        alert('right or left');
      }
    }
  }
}
