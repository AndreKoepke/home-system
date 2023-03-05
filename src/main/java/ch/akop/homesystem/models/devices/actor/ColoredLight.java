package ch.akop.homesystem.models.devices.actor;


import ch.akop.homesystem.deconz.rest.State;
import ch.akop.homesystem.models.color.Color;
import ch.akop.homesystem.util.SleepUtil;
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

    private Color currentColor;

    public ColoredLight(BiConsumer<Integer, Duration> functionToSeBrightness,
                        Consumer<Boolean> functionToTurnOnOrOff,
                        BiConsumer<Color, Duration> functionForRgb) {
        super.setFunctionToSeBrightness(functionToSeBrightness);
        super.setFunctionToTurnOnOrOff(functionToTurnOnOrOff);
        this.functionForRgb = functionForRgb;
    }

    public void setColorAndBrightness(Color color, Duration transitionTime, BigDecimal brightness) {
        functionForRgb.accept(color, null);
        // timeout is necessary, because lamps and/or deconz can't handle it
        SleepUtil.sleep(Duration.ofMillis(300));
        super.setBrightness(brightness, transitionTime);
    }

    @Override
    protected void consumeInternalUpdate(State update) {
        super.consumeInternalUpdate(update);
        currentColor = Color.fromXY(update.getXy(), update.getBri());
    }

}
