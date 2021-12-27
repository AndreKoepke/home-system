package ch.akop.homesystem.models.animation.config;

import ch.akop.homesystem.models.animation.steps.AnimationStep;
import ch.akop.homesystem.models.devices.actor.Light;
import lombok.Data;

import java.util.Set;

@Data
public class AnimationConfig {

    private LightsConfig lights;
    private PauseConfig pause;


    public AnimationStep toAnimationStep(final Set<Light> allLights) {
        if (this.lights != null && this.pause == null) {
            return this.lights.toStep(allLights);
        } else if (this.lights == null && this.pause != null) {
            return this.pause.toPauseStep();
        } else if (this.lights != null) {
            throw new IllegalArgumentException("Lights and Pause together. This is illegal. This incident will be reported!");
        } else {
            throw new IllegalArgumentException("No Lights nor Pause set. This is illegal. This incident will be reported!");
        }
    }

}
