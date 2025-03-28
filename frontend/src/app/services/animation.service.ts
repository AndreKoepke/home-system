import {Injectable} from '@angular/core';
import {HttpClient} from "@angular/common/http";
import {environment} from "../../environments/environment";
import {AnimationDto} from "../models/animation.dto";
import {EMPTY, mergeMap, Observable} from "rxjs";

@Injectable({
  providedIn: 'root'
})
export class AnimationService {

  constructor(private httpClient: HttpClient) {
  }

  public getAnimations$(): Observable<AnimationDto[]> {
    return this.httpClient.get<AnimationDto[]>(`${this.backendUrl}secured/v1/animations/`)
  }

  public playAnimation$(animationId: string): Observable<void> {
    return this.httpClient.post(`${this.backendUrl}secured/v1/animations/start/${animationId}`, null)
      .pipe(mergeMap(() => EMPTY));
  }

  private get backendUrl(): string {
    return `${environment.backend.protocol}${environment.backend.host}/${environment.backend.path}`;
  }
}
