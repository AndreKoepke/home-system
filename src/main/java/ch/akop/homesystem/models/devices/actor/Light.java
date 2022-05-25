package ch.akop.homesystem.models.devices.actor;

import ch.akop.homesystem.models.devices.Device;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class Light extends Device<Light> {

    @Getter(AccessLevel.PROTECTED)
    @Setter(AccessLevel.PROTECTED)
    private BiConsumer<Integer, Duration> functionToSeBrightness;

    @Getter(AccessLevel.PROTECTED)
    @Setter(AccessLevel.PROTECTED)
    private Consumer<Boolean> functionToTurnOnOrOff;

    private boolean on;

    public Light(BiConsumer<Integer, Duration> functionToSeBrightness, Consumer<Boolean> functionToTurnOnOrOff) {
        this.functionToSeBrightness = functionToSeBrightness;
        this.functionToTurnOnOrOff = functionToTurnOnOrOff;
    }

    public void setBrightness(final BigDecimal decimal, final Duration transitionTime) {
        this.setBrightness(decimal.multiply(BigDecimal.valueOf(100)).intValue(), transitionTime);
    }

    public void setBrightness(final int percent, final Duration transitionTime) {
        this.getFunctionToSeBrightness().accept(percent, transitionTime);
    }
}
