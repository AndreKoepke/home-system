import {ChangeDetectionStrategy, Component, input, Output, OutputEmitterRef} from '@angular/core';
import {AnimationDto} from "../../models/animation.dto";
import {
  SbbExpansionPanel,
  SbbExpansionPanelContent,
  SbbExpansionPanelHeader
} from "@sbb-esta/lyne-angular/expansion-panel";
import {SbbMiniButton} from "@sbb-esta/lyne-angular/button";
import {SbbAccordion} from "@sbb-esta/lyne-angular/accordion";

@Component({
  selector: 'app-animation-page',
  standalone: true,
  imports: [
    SbbExpansionPanelContent,
    SbbMiniButton,
    SbbExpansionPanelHeader,
    SbbExpansionPanel,
    SbbAccordion
  ],
  templateUrl: './animation-page.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './animation-page.component.scss'
})
export class AnimationPageComponent {

  public animations = input.required<AnimationDto[]>();

  @Output()
  public startAnimation = new OutputEmitterRef<string>();
}
