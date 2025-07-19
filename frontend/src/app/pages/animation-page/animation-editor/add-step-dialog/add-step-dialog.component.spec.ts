import {ComponentFixture, TestBed} from '@angular/core/testing';

import {AddStepDialogComponent} from './add-step-dialog.component';

describe('AddStepDialogComponent', () => {
  let component: AddStepDialogComponent;
  let fixture: ComponentFixture<AddStepDialogComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AddStepDialogComponent]
    })
      .compileComponents();

    fixture = TestBed.createComponent(AddStepDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
