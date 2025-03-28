import {ChangeDetectionStrategy, Component} from '@angular/core';

@Component({
  selector: 'app-weather-page',
  standalone: true,
  imports: [],
  templateUrl: './weather-page.component.html',
  styleUrl: './weather-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class WeatherPageComponent {

}
