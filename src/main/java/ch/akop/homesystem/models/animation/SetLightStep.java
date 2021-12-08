package ch.akop.homesystem.models.animation;

import ch.akop.homesystem.models.devices.actor.Light;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

@Data
@EqualsAndHashCode(callSuper = false)
public class SetLightStep extends AnimationStep {

    private final Light light;
    private Duration transitionTime = Duration.of(2, ChronoUnit.SECONDS);
    private BigDecimal brightness = BigDecimal.ONE;

    @Override
    void play() {
        this.light.setBrightness(brightness.multiply(BigDecimal.valueOf(255)).intValue(), transitionTime);
    }

}
