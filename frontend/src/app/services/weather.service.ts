import {DestroyRef, Injectable} from '@angular/core';
import {ReplaySubject, retry} from "rxjs";
import {webSocket} from "rxjs/webSocket";
import {takeUntilDestroyed} from "@angular/core/rxjs-interop";
import {Weather} from "../models/devices/weather.dto";
import {getWebsocketBaseUrl} from "../url-resolver";

@Injectable({
  providedIn: 'root'
})
export class WeatherService {

  private weatherSubject = new ReplaySubject<Weather>(1);

  public get weather$() {
    return this.weatherSubject;
  }

  private webSocketSubject = webSocket<Weather>({
    url: `${getWebsocketBaseUrl()}weather`,
    openObserver: {
      next: () => {
      },
      error: err => console.error('WebSocketConnection threw error', err),
      complete: () => console.warn('WebSocketConnection was closed')
    }
  });

  constructor(private destroyRef: DestroyRef) {
    this.setupWebsocketListener();
  }

  private setupWebsocketListener() {
    this.webSocketSubject
      .pipe(
        retry({delay: 5000}),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe(message => {
        this.weatherSubject.next(message);
      });
  }
}
