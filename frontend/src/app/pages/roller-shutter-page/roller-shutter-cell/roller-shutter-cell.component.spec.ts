import {ComponentFixture, TestBed} from '@angular/core/testing';

import {RollerShutterCellComponent} from './roller-shutter-cell.component';

describe('RollerShutterCellComponent', () => {
  let component: RollerShutterCellComponent;
  let fixture: ComponentFixture<RollerShutterCellComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RollerShutterCellComponent]
    })
      .compileComponents();

    fixture = TestBed.createComponent(RollerShutterCellComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
