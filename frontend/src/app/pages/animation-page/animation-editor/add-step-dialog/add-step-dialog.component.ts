import {ChangeDetectionStrategy, Component} from '@angular/core';
import {MatFormField, MatLabel} from "@angular/material/form-field";
import {MatDialogRef} from "@angular/material/dialog";
import {MatOption, MatSelect} from "@angular/material/select";

@Component({
  selector: 'app-add-step-dialog',
  standalone: true,
  imports: [
    MatFormField,
    MatSelect,
    MatOption,
    MatLabel
  ],
  templateUrl: './add-step-dialog.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './add-step-dialog.component.scss'
})
export class AddStepDialogComponent {

  public newKindOfAnimation: string | undefined;

  public constructor(public dialogRef: MatDialogRef<AddStepDialogComponent>) {
  }

  public onNoClick() {
    this.dialogRef.close();
  }

  onSave() {
    this.dialogRef.close(this.newKindOfAnimation);
  }
}
