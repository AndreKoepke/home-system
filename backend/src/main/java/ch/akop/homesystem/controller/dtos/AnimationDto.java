package ch.akop.homesystem.controller.dtos;

import ch.akop.homesystem.persistence.model.animation.Animation;
import ch.akop.homesystem.persistence.model.animation.steps.Step;
import java.util.List;
import java.util.UUID;
import lombok.Data;

@Data
public class AnimationDto {

  private UUID id;
  private String name;
  private List<StepDto> steps;

  public static AnimationDto from(Animation animation) {
    return new AnimationDto()
        .setId(animation.getId())
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

    public static StepDto from(Step step) {
      return new StepDto()
          .setId(step.getId())
          .setSortOrder(step.getSortOrder())
          .setActionDescription(step.getActionDescription())
          .setAffectedLight(step.getNameOfLight());
    }
  }
}
