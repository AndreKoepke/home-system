package ch.akop.homesystem.models.animation.config;

import ch.akop.homesystem.models.animation.steps.AnimationStep;
import ch.akop.homesystem.models.devices.actor.SimpleLight;
import lombok.Value;

import java.util.Set;

@Value
public class AnimationConfig {

    LightsConfig lights;
    PauseConfig pause;


    public AnimationStep toAnimationStep(Set<SimpleLight> affectedLights) {
        if (lights != null && pause == null) {
            return lights.toStep(affectedLights);
        } else if (lights == null && pause != null) {
            return pause.toPauseStep();
        } else if (lights != null) {
            throw new IllegalArgumentException("Lights and Pause together. This is illegal. This incident will be reported!");
        } else {
            throw new IllegalArgumentException("No Lights nor Pause set. This is illegal. This incident will be reported!");
        }
    }

}
