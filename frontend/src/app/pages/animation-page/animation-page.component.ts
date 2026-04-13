import {Component, input, Output, OutputEmitterRef} from '@angular/core';
import {AnimationDto} from "../../models/animation.dto";
import {SbbMiniButton} from "@sbb-esta/lyne-angular/button/mini-button";
import {SbbAccordion} from "@sbb-esta/lyne-angular/accordion";
import {
  SbbExpansionPanel,
  SbbExpansionPanelContent,
  SbbExpansionPanelHeader
} from "@sbb-esta/lyne-angular/expansion-panel";

@Component({
  selector: 'app-animation-page',
  standalone: true,
  imports: [
    SbbAccordion,
    SbbExpansionPanel,
    SbbExpansionPanelHeader,
    SbbExpansionPanelContent,
    SbbMiniButton
  ],
  templateUrl: './animation-page.component.html',
  styleUrl: './animation-page.component.scss'
})
export class AnimationPageComponent {

  public animations = input.required<AnimationDto[]>();

  @Output()
  public startAnimation = new OutputEmitterRef<string>();
}
