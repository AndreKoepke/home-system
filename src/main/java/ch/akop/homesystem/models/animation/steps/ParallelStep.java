package ch.akop.homesystem.models.animation.steps;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Collection;

@Data
@EqualsAndHashCode(callSuper = false)
public class ParallelStep implements AnimationStep {

    private final Collection<AnimationStep> steps;


    @Override
    public void play() {
        this.steps.parallelStream().forEach(AnimationStep::play);
    }
}
