import {ComponentFixture, TestBed} from '@angular/core/testing';

import {RollerShutterPageComponent} from './roller-shutter-page.component';

describe('RollerShutterPageComponent', () => {
  let component: RollerShutterPageComponent;
  let fixture: ComponentFixture<RollerShutterPageComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RollerShutterPageComponent]
    })
      .compileComponents();

    fixture = TestBed.createComponent(RollerShutterPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
