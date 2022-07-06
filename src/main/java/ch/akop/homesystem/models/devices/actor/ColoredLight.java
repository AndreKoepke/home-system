package ch.akop.homesystem.models.devices.actor;


import ch.akop.homesystem.models.color.Color;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.Duration;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ColoredLight extends DimmableLight {

    @ToString.Exclude
    private final BiConsumer<Color, Duration> functionForRgb;

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
}
