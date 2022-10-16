package ch.akop.homesystem.models.devices.actor;

import ch.akop.homesystem.deconz.rest.State;
import lombok.*;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@NoArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class DimmableLight extends SimpleLight {

    @Getter(AccessLevel.PROTECTED)
    @Setter(AccessLevel.PROTECTED)
    @ToString.Exclude
    private BiConsumer<Integer, Duration> functionToSeBrightness;

    @Getter
    @Setter
    private int currentBrightness;

    public DimmableLight(BiConsumer<Integer, Duration> functionToSeBrightness, Consumer<Boolean> functionToTurnOnOrOff) {
        super(functionToTurnOnOrOff);
        this.functionToSeBrightness = functionToSeBrightness;
    }

    public void setBrightness(int percent, Duration transitionTime) {
        this.getFunctionToSeBrightness().accept(percent, transitionTime);
    }

    public void setBrightness(BigDecimal decimal, Duration transitionTime) {
        this.getFunctionToSeBrightness().accept(decimal.multiply(BigDecimal.valueOf(100)).intValue(), transitionTime);
    }

    @Override
    public DimmableLight consumeUpdate(State update) {
        super.consumeUpdate(update);
        setCurrentBrightness(update.getBri());

        return this;
    }
}
