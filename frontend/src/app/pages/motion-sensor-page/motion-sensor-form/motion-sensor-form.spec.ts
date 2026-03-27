import {ComponentFixture, TestBed} from '@angular/core/testing';

import {MotionSensorForm} from './motion-sensor-form';

describe('MotionSensorForm', () => {
  let component: MotionSensorForm;
  let fixture: ComponentFixture<MotionSensorForm>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MotionSensorForm]
    })
      .compileComponents();

    fixture = TestBed.createComponent(MotionSensorForm);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
