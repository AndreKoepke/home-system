package ch.akop.homesystem.models.animation.steps;

import lombok.Data;

import java.util.Collection;

@Data
public class ParallelStep implements AnimationStep {

    private final Collection<AnimationStep> steps;


    @Override
    public void play() {
        this.steps.parallelStream().forEach(AnimationStep::play);
    }
}
