import {ChangeDetectionStrategy, Component, computed, input, Output, OutputEmitterRef} from '@angular/core';
import {AnimationDto, Step} from "../../../models/animation.dto";
import {CircleButtonComponent} from "../../../components/buttons/circle-button/circle-button.component";
import {MatDialog} from "@angular/material/dialog";
import {AddStepDialogComponent} from "./add-step-dialog/add-step-dialog.component";
import {filter, Observable, take} from "rxjs";

@Component({
  selector: 'app-animation-editor',
  standalone: true,
  imports: [
    CircleButtonComponent
  ],
  templateUrl: './animation-editor.component.html',
  styleUrl: './animation-editor.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AnimationEditorComponent {

  animation = input.required<AnimationDto>();
  sortedSteps = computed(() => this.animation().steps.sort(step => step.sortOrder));

  @Output('createStep')
  public createStep = new OutputEmitterRef<{
    parent: string,
    kind: string
  }>();

  constructor(public dialog: MatDialog) {
  }

  addStepBefore(step: Step): void {
    this.openDialog()
      .pipe(
        take(1),
        filter(value => !!value)
      )
      .subscribe(result => {
        this.createStep.emit({
          parent: step.id,
          kind: result
        });
      })
  }

  public openDialog(): Observable<any> {
    const dialogRef = this.dialog.open(AddStepDialogComponent, {
      width: '33vw',
    });

    return dialogRef.afterClosed();
  }
}
