import {Component} from '@angular/core';
import {MatFormField, MatLabel} from "@angular/material/form-field";
import {MatButton} from "@angular/material/button";
import {MatDialogActions, MatDialogContent, MatDialogRef, MatDialogTitle} from "@angular/material/dialog";
import {MatOption, MatSelect} from "@angular/material/select";

@Component({
  selector: 'app-add-step-dialog',
  standalone: true,
  imports: [
    MatFormField,
    MatButton,
    MatDialogActions,
    MatDialogContent,
    MatDialogTitle,
    MatSelect,
    MatOption,
    MatLabel
  ],
  templateUrl: './add-step-dialog.component.html',
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
