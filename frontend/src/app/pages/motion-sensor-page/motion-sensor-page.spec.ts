import {ComponentFixture, TestBed} from '@angular/core/testing';

import {MotionSensorPage} from './motion-sensor-page';

describe('MotionSensorPage', () => {
  let component: MotionSensorPage;
  let fixture: ComponentFixture<MotionSensorPage>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MotionSensorPage]
    })
      .compileComponents();

    fixture = TestBed.createComponent(MotionSensorPage);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
