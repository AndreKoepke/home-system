package ch.akop.homesystem.models.animation.steps;

import ch.akop.homesystem.models.devices.actor.Light;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

@Data
@EqualsAndHashCode(callSuper = false)
public class SetLightStep implements AnimationStep {

    private final Light light;
    private Duration transitionTime = Duration.of(2, ChronoUnit.SECONDS);
    private BigDecimal brightness = BigDecimal.ONE;

    @Override
    public void play() {
        this.light.setBrightness(this.brightness, this.transitionTime);
    }

}
