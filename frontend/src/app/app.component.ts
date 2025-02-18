import {ChangeDetectionStrategy, Component, HostListener, Inject, PLATFORM_ID, Signal} from '@angular/core';
import {NavigationEnd, Router, RouterOutlet} from '@angular/router';
import {toSignal} from "@angular/core/rxjs-interop";
import {filter, map, Subscription, timer} from "rxjs";
import {isPlatformBrowser} from "@angular/common";

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AppComponent {
  title = 'home-system';

  public readonly routes: { text: string; link: string }[] = [
    {text: 'Home', link: '/'},
    {text: 'Zug', link: '/train'},
    {text: 'Wetter', link: '/weather'},
    {text: 'Roggen', link: '/livecam'},
    {text: 'Trello', link: '/trello'}
  ];
  public activeRoute: Signal<string | undefined>;

  private nextMenuSubscription: Subscription | undefined;
  private readonly intervalForNextMenu = 60_000;


  public constructor(private router: Router, @Inject(PLATFORM_ID) plattformId: Object) {
    this.activeRoute = toSignal(router.events.pipe(
      filter(event => event instanceof NavigationEnd),
      map(event => (event as NavigationEnd).url),
    ));

    if (isPlatformBrowser(plattformId)) {
      this.nextMenuSubscription = timer(this.intervalForNextMenu).subscribe(() => this.nextMenuByTimer());
    }
  }

  private swipeCoord: [number, number] = [0, 0];
  private swipeTime: number = 0;

  public clickMenuItem(link: string): void {
    this.router.navigate([link]);
    this.nextMenuSubscription?.unsubscribe();
    this.nextMenuSubscription = timer(this.intervalForNextMenu * 2).subscribe(() => this.nextMenuByTimer());
  }

  private nextMenuByTimer(): void {
    let currentRouteIndex = this.routes
      .findIndex(configuredRoute => configuredRoute.link === (this.activeRoute() ?? '/'));
    this.router.navigate([this.getLinkToNextMenuItem(currentRouteIndex)]);
    this.nextMenuSubscription?.unsubscribe();
    this.nextMenuSubscription = timer(this.intervalForNextMenu).subscribe(() => this.nextMenuByTimer());
  }

  private getLinkToNextMenuItem(current: number): string {
    return this.routes[(current + 1) % this.routes.length].link;
  }

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
