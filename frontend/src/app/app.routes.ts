import {Routes} from '@angular/router';
import {StartPageComponent} from "./pages/start-page/start-page.component";
import {TrainStationPageComponent} from "./pages/train-station-page/train-station-page.component";

export const routes: Routes = [{
  path: '',
  component: StartPageComponent
},
  {
    path: 'train',
    component: TrainStationPageComponent
  }];
