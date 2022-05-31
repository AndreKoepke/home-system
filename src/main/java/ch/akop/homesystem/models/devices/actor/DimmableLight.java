package ch.akop.homesystem.models.devices.actor;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@NoArgsConstructor
public class DimmableLight extends SimpleLight {

    @Getter(AccessLevel.PROTECTED)
    @Setter(AccessLevel.PROTECTED)
    private BiConsumer<Integer, Duration> functionToSeBrightness;

    public DimmableLight(BiConsumer<Integer, Duration> functionToSeBrightness, Consumer<Boolean> functionToTurnOnOrOff) {
        super(functionToTurnOnOrOff);
        this.functionToSeBrightness = functionToSeBrightness;
    }

    public void setBrightness(final int percent, final Duration transitionTime) {
        this.getFunctionToSeBrightness().accept(percent, transitionTime);
    }

    public void setBrightness(final BigDecimal decimal, final Duration transitionTime) {
        this.getFunctionToSeBrightness().accept(decimal.multiply(BigDecimal.valueOf(100)).intValue(), transitionTime);
    }
}
