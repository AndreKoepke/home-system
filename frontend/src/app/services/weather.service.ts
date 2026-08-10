import {DestroyRef, inject, Injectable} from '@angular/core';
import {filter, ReplaySubject, retry} from "rxjs";
import {webSocket} from "rxjs/webSocket";
import {takeUntilDestroyed} from "@angular/core/rxjs-interop";
import {Weather} from "../models/devices/weather.dto";
import {getWebsocketBaseUrl} from "../url-resolver";
import {AuthService} from "../core/auth.service";
import {ControlMessage, isNotControlMessage} from "./devices.service";

@Injectable({
  providedIn: 'root'
})
export class WeatherService {

  private readonly authService = inject(AuthService);
  private weatherSubject = new ReplaySubject<Weather>(1);

  public get weather$() {
    return this.weatherSubject;
  }

  private webSocketSubject = webSocket<Weather | ControlMessage>({
    url: `${getWebsocketBaseUrl()}weather`,
    openObserver: {
      next: () => {
        this.webSocketSubject.next({type: 'auth', token: this.authService.apiKey!});
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
        takeUntilDestroyed(this.destroyRef),
        filter(isNotControlMessage)
      )
      .subscribe(message => {
        this.weatherSubject.next(message);
      });
  }
}
