import {ChangeDetectionStrategy, Component} from "@angular/core";
import {AsyncPipe} from "@angular/common";
import {Observable} from "rxjs";
import {AnimationService} from "../../services/animation.service";
import {AnimationDto} from "../../models/animation.dto";
import {AnimationPageComponent} from "./animation-page.component";

@Component({
  standalone: true,
  imports: [
    AsyncPipe,
    AnimationPageComponent
  ],
  template: `
    @if (animations$ | async; as animations) {
      <app-animation-page
        [animations]="animations"
        (startAnimation)="animationService.playAnimation$($event).subscribe()"
      />
    }`,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AnimationPageContainerComponent {

  animations$: Observable<AnimationDto[]>;

  constructor(public animationService: AnimationService) {
    this.animations$ = animationService.getAnimations$();
  }
}
