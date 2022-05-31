package ch.akop.homesystem.models.animation.steps;

import ch.akop.homesystem.models.devices.actor.DimmableLight;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

@Data
public class DimmLightStep implements AnimationStep {

    private final DimmableLight light;
    private Duration transitionTime = Duration.of(2, ChronoUnit.SECONDS);
    private BigDecimal brightness = BigDecimal.ONE;

    @Override
    public void play() {
        this.light.setBrightness(this.brightness, this.transitionTime);
    }

}
