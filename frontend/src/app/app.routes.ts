import {Routes} from '@angular/router';
import {TrainStationPageComponent} from "./pages/train-station-page/train-station-page.component";
import {WeatherPageComponent} from "./pages/weather-page/weather-page.component";
import {UnauthorizedPageComponent} from "./pages/unauthorized-page/unauthorized-page.component";
import {AuthGuard} from "./core/auth.service";
import {StartPageContainerComponent} from "./pages/start-page/start-page-container.component";

export const routes: Routes = [
  {
    path: '',
    component: StartPageContainerComponent,
    canActivate: [AuthGuard]
  },
  {
    path: 'train',
    component: TrainStationPageComponent,
    canActivate: [AuthGuard]
  },
  {
    path: 'weather',
    component: WeatherPageComponent,
    canActivate: [AuthGuard]
  },
  {
    path: 'unauthorized',
    component: UnauthorizedPageComponent
  }
];
