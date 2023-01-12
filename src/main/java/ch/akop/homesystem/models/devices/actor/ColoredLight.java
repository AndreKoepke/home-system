package ch.akop.homesystem.models.devices.actor;


import ch.akop.homesystem.deconz.rest.State;
import ch.akop.homesystem.models.color.Color;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.Duration;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ColoredLight extends DimmableLight {

    @ToString.Exclude
    private final BiConsumer<Color, Duration> functionForRgb;

    private Color currentColor;

    public ColoredLight(BiConsumer<Integer, Duration> functionToSeBrightness,
                        Consumer<Boolean> functionToTurnOnOrOff,
                        BiConsumer<Color, Duration> functionForRgb) {
        super.setFunctionToSeBrightness(functionToSeBrightness);
        super.setFunctionToTurnOnOrOff(functionToTurnOnOrOff);
        this.functionForRgb = functionForRgb;
    }

    public void setColor(Color color, Duration transitionTime) {
        this.getFunctionForRgb().accept(color, transitionTime);
    }

    @Override
    protected void consumeInternalUpdate(State update) {
        super.consumeInternalUpdate(update);
        currentColor = Color.fromXY(update.getXy(), update.getBri());
    }

}
