import {ComponentFixture, TestBed} from '@angular/core/testing';

import {TrainStationPageComponent} from './train-station-page.component';

describe('TrainStationPageComponent', () => {
  let component: TrainStationPageComponent;
  let fixture: ComponentFixture<TrainStationPageComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TrainStationPageComponent]
    })
      .compileComponents();

    fixture = TestBed.createComponent(TrainStationPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
