import {inject, Injectable} from "@angular/core";
import {HttpClient} from "@angular/common/http";
import {EMPTY, mergeMap, Observable} from "rxjs";
import {TimerConfig} from "../models/timer-config.dto";
import {getHttpBaseUrl} from "../url-resolver";

@Injectable({
  providedIn: 'root'
})
export class TimerService {

  private readonly http = inject(HttpClient);

  public getConfigs(): Observable<TimerConfig[]> {
    return this.http.get<TimerConfig[]>(`${getHttpBaseUrl()}v1/timer`)
  }

  public saveConfig(config: TimerConfig): Observable<never> {
    return this.http.post<void>(`${getHttpBaseUrl()}v1/timer`, config)
      .pipe(mergeMap(() => EMPTY))
  }

}
