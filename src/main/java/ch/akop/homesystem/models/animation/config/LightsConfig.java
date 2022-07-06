package ch.akop.homesystem.models.animation.config;

import ch.akop.homesystem.models.animation.steps.AnimationStep;
import ch.akop.homesystem.models.animation.steps.DimmLightStep;
import ch.akop.homesystem.models.animation.steps.OnOffStep;
import ch.akop.homesystem.models.animation.steps.ParallelStep;
import ch.akop.homesystem.models.devices.actor.DimmableLight;
import ch.akop.homesystem.models.devices.actor.SimpleLight;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Set;

@Data
public class LightsConfig {

    private List<String> names;
    private BigDecimal toBrightness;
    private Duration transitionTime;


    public AnimationStep toStep(final Set<SimpleLight> affectedLights) {
        if (affectedLights.size() > 1) {
            return new ParallelStep(affectedLights.stream()
                    .map(this::getLightStep)
                    .toList());
        }

        return getLightStep(affectedLights.stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No light found for %s".formatted(String.join(", ", names)))));
    }

    private AnimationStep getLightStep(final SimpleLight light) {

        // new switch pattern can replace this ugly if in future
        if (light instanceof DimmableLight dimmable) {
            return new DimmLightStep(dimmable)
                    .setBrightness(this.toBrightness)
                    .setTransitionTime(this.transitionTime);
        }

        return new OnOffStep(light)
                .setTurnLightOn(this.toBrightness.compareTo(BigDecimal.ZERO) > 0);
    }

}
