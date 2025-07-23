import {ChangeDetectionStrategy, Component, input, Output, OutputEmitterRef} from '@angular/core';

@Component({
  selector: 'app-circle-button',
  standalone: true,
  imports: [],
  templateUrl: './circle-button.component.html',
  styleUrl: './circle-button.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class CircleButtonComponent {

  public text = input.required<string>();

  @Output()
  public triggered = new OutputEmitterRef<void>();

  public keyPressed(event: KeyboardEvent): void {
    if (event.key === 'enter' || event.key === 'space') {
      this.triggered.emit();
    }
  }
}
