import {ComponentFixture, TestBed} from '@angular/core/testing';

import {LivecamPageComponent} from './livecam-page.component';

describe('LivecamPageComponent', () => {
  let component: LivecamPageComponent;
  let fixture: ComponentFixture<LivecamPageComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LivecamPageComponent]
    })
      .compileComponents();

    fixture = TestBed.createComponent(LivecamPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
