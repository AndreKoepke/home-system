import {ChangeDetectionStrategy, Component} from '@angular/core';

@Component({
  selector: 'app-livecam-page',
  standalone: true,
  imports: [],
  templateUrl: './livecam-page.component.html',
  styleUrl: './livecam-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class LivecamPageComponent {

}
