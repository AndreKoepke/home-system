import {Injectable} from '@angular/core';
import {HttpClient} from "@angular/common/http";
import {AnimationDto} from "../models/animation.dto";
import {EMPTY, map, mergeMap, Observable} from "rxjs";
import {getHttpBaseUrl} from "../url-resolver";

@Injectable({
  providedIn: 'root'
})
export class AnimationService {

  constructor(private httpClient: HttpClient) {
  }

  public getAnimations$(): Observable<AnimationDto[]> {
    return this.httpClient.get<AnimationDto[]>(`${getHttpBaseUrl()}v1/animations/`)
      .pipe(map(animations => animations.map(animation => Object.assign({}, animation))))
  }

  public playAnimation$(animationId: string): Observable<void> {
    return this.httpClient.post(`${getHttpBaseUrl()}v1/animations/start/${animationId}`, null)
      .pipe(mergeMap(() => EMPTY));
  }
}
