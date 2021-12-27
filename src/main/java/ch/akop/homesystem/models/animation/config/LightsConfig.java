package ch.akop.homesystem.models.animation.config;

import ch.akop.homesystem.models.animation.steps.AnimationStep;
import ch.akop.homesystem.models.animation.steps.ParallelStep;
import ch.akop.homesystem.models.animation.steps.SetLightStep;
import ch.akop.homesystem.models.devices.actor.Light;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Data
public class LightsConfig {

    private List<String> names;
    private BigDecimal toBrightness;
    private Duration transitionTime;


    public AnimationStep toStep(final Set<Light> allLights) {
        if (this.names.size() > 1) {
            return new ParallelStep(this.names.stream()
                    .map(name -> findLight(name, allLights))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .map(this::getLightStep)
                    .collect(Collectors.toList()));
        }

        return getLightStep(findLight(this.names.get(0), allLights)
                .orElseThrow(() -> new NoSuchElementException("No light found for: %s".formatted(this.names.get(0)))));
    }

    private SetLightStep getLightStep(final Light light) {
        return new SetLightStep(light)
                .setBrightness(this.toBrightness)
                .setTransitionTime(this.transitionTime);
    }


    private Optional<Light> findLight(final String lightName, final Set<Light> allLights) {
        return allLights.stream()
                .filter(light -> light.getName().equals(lightName) || light.getId().equals(lightName))
                .findFirst();
    }


}
