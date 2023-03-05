package ch.akop.homesystem.models.devices.actor;


import ch.akop.homesystem.deconz.rest.State;
import ch.akop.homesystem.models.color.Color;
import io.smallrye.mutiny.tuples.Functions;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ColoredLight extends DimmableLight {

    @ToString.Exclude
    private final BiConsumer<Color, Duration> functionForRgb;

    @ToString.Exclude
    private final Functions.TriConsumer<Color, Duration, Integer> functionForAllFields;

    private Color currentColor;

    public ColoredLight(BiConsumer<Integer, Duration> functionToSeBrightness,
                        Consumer<Boolean> functionToTurnOnOrOff,
                        BiConsumer<Color, Duration> functionForRgb,
                        Functions.TriConsumer<Color, Duration, Integer> functionAllFields) {
        super.setFunctionToSeBrightness(functionToSeBrightness);
        super.setFunctionToTurnOnOrOff(functionToTurnOnOrOff);
        this.functionForRgb = functionForRgb;
        this.functionForAllFields = functionAllFields;
    }

    public void setColorAndBrightness(Color color, Duration transitionTime, BigDecimal brightness) {
        this.getFunctionForAllFields().accept(color, transitionTime, brightness.multiply(BigDecimal.valueOf(100)).intValue());
    }

    @Override
    protected void consumeInternalUpdate(State update) {
        super.consumeInternalUpdate(update);
        currentColor = Color.fromXY(update.getXy(), update.getBri());
    }

}
