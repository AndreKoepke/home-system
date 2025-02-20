import {Component, input, Output, OutputEmitterRef} from '@angular/core';
import {AnimationDto} from "../../models/animation.dto";

@Component({
  selector: 'app-animation-page',
  standalone: true,
  imports: [],
  templateUrl: './animation-page.component.html',
  styleUrl: './animation-page.component.scss'
})
export class AnimationPageComponent {

  public animations = input.required<AnimationDto[]>();

  @Output()
  public startAnimation = new OutputEmitterRef<string>();

}
