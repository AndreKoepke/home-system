import {inject, Injectable} from "@angular/core";
import {HttpClient} from "@angular/common/http";
import {EMPTY, mergeMap, Observable} from "rxjs";
import {TimerConfig} from "../models/timer-config.dto";
import {environment} from "../../environments/environment";
import {baseUrl} from "./devices.service";

@Injectable({
  providedIn: 'root'
})
export class TimerService {

  private readonly http = inject(HttpClient);

  public getConfigs(): Observable<TimerConfig[]> {
    return this.http.get<TimerConfig[]>(`${environment.backend.protocol}${baseUrl()}/v1/timer`)
  }

  public saveConfig(config: TimerConfig): Observable<never> {
    return this.http.post<void>(`${environment.backend.protocol}${baseUrl()}/v1/timer`, config)
      .pipe(mergeMap(() => EMPTY))
  }

}
