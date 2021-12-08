package ch.akop.homesystem.models.animation;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
public class ParallelStep extends AnimationStep {

    private final List<AnimationStep> steps;

    public ParallelStep(AnimationStep... steps) {
        this.steps = List.of(steps);
    }

    public ParallelStep(List<AnimationStep> steps) {
        this.steps = steps;
    }

    @Override
    void play() {
        steps.parallelStream().forEach(AnimationStep::play);
    }
}
