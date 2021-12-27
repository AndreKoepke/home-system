package ch.akop.homesystem.models.animation;

import ch.akop.homesystem.models.animation.steps.AnimationStep;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Data
@Slf4j
public class Animation {

    private List<AnimationStep> animationSteps;


    public void play() {
        for (final AnimationStep animationStep : this.animationSteps) {
            log.debug("Started {}", animationStep);
            animationStep.play();
            log.debug("Stopped {}", animationStep);
        }
    }
}
