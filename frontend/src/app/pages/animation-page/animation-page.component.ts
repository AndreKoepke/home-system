import {Component, input, Output, OutputEmitterRef} from '@angular/core';
import {AnimationDto} from "../../models/animation.dto";
import {CircleButtonComponent} from "../../components/buttons/circle-button/circle-button.component";

@Component({
  selector: 'app-animation-page',
  standalone: true,
  imports: [
    CircleButtonComponent
  ],
  templateUrl: './animation-page.component.html',
  styleUrl: './animation-page.component.scss'
})
export class AnimationPageComponent {

  public animations = input.required<AnimationDto[]>();

  @Output()
  public startAnimation = new OutputEmitterRef<string>();

}
