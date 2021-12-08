package ch.akop.homesystem.models.animation;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Data
@Slf4j
public class Animation {

    private List<AnimationStep> animationSteps;


    public void play() {
        for (AnimationStep animationStep : animationSteps) {
            log.debug("Started {}", animationStep);
            animationStep.play();
            log.debug("Stopped {}", animationStep);
        }
    }

    public Animation addAStep(AnimationStep step) {
        this.animationSteps.add(step);
        return this;
    }
}
