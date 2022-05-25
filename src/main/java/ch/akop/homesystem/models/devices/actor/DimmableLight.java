package ch.akop.homesystem.models.devices.actor;

import lombok.NoArgsConstructor;

import java.time.Duration;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@NoArgsConstructor
public class DimmableLight extends Light {
    public DimmableLight(BiConsumer<Integer, Duration> functionToSeBrightness, Consumer<Boolean> functionToTurnOnOrOff) {
        super(functionToSeBrightness, functionToTurnOnOrOff);
    }

    public void setBrightness(final int percent, final Duration transitionTime) {
        this.getFunctionToSeBrightness().accept(percent, transitionTime);
    }
}
