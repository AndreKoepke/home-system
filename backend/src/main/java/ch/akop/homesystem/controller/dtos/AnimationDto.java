package ch.akop.homesystem.controller.dtos;

import ch.akop.homesystem.persistence.model.animation.Animation;
import ch.akop.homesystem.persistence.model.animation.steps.PauseStep;
import ch.akop.homesystem.persistence.model.animation.steps.Step;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import lombok.Data;

@Data
public class AnimationDto implements Identable {

  private String id;
  private String name;
  private List<StepDto> steps;

  public static AnimationDto from(Animation animation) {
    return new AnimationDto()
        .setId(animation.getId().toString())
        .setName(animation.getName())
        .setSteps(animation.materializeSteps().stream()
            .map(StepDto::from)
            .toList());
  }

  @Data
  public static class StepDto {

    private UUID id;
    private Integer sortOrder;
    private String actionDescription;
    private String affectedLight;
    private Duration runTime;

    public static StepDto from(Step step) {
      return new StepDto()
          .setId(step.getId())
          .setSortOrder(step.getSortOrder())
          .setActionDescription(step.getActionDescription())
          .setAffectedLight(step.getNameOfLight())
          .setRunTime(getRunTime(step));
    }

    public static Duration getRunTime(Step step) {
      if (step instanceof PauseStep pauseStep) {
        return pauseStep.getWaitFor();
      }

      return Duration.ZERO;
    }
  }
}
