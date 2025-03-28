import {Routes} from '@angular/router';
import {TrainStationPageComponent} from "./train-station-page/train-station-page.component";
import {WeatherPageComponent} from "./weather-page/weather-page.component";
import {UnauthorizedPageComponent} from "./unauthorized-page/unauthorized-page.component";
import {AuthGuard} from "../core/auth.service";
import {StartPageContainerComponent} from "./start-page/start-page-container.component";
import {LivecamPageComponent} from "./livecam-page/livecam-page.component";
import {TrelloPageComponent} from "./trello-page/trello-page.component";
import {AnimationPageContainerComponent} from "./animation-page/animation-page-container.component";
import {RollerShutterPageContainerComponent} from "./roller-shutter-page/roller-shutter-page-container.component";

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
    path: 'livecam',
    component: LivecamPageComponent,
    canActivate: [AuthGuard]
  },
  {
    path: 'animation',
    component: AnimationPageContainerComponent,
    canActivate: [AuthGuard]
  },
  {
    path: 'roller-shutter',
    component: RollerShutterPageContainerComponent,
    canActivate: [AuthGuard]
  },
  {
    path: 'trello',
    component: TrelloPageComponent,
    canActivate: [AuthGuard]
  },
  {
    path: 'unauthorized',
    component: UnauthorizedPageComponent
  }
];
