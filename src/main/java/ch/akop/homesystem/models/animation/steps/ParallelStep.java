package ch.akop.homesystem.models.animation.steps;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
public class ParallelStep implements AnimationStep {

    private final List<AnimationStep> steps;

    public ParallelStep(final List<AnimationStep> steps) {
        this.steps = steps;
    }

    @Override
    public void play() {
        this.steps.parallelStream().forEach(AnimationStep::play);
    }
}
