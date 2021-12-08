package ch.akop.homesystem.models.devices.actor;

import ch.akop.homesystem.models.devices.Device;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.time.Duration;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@EqualsAndHashCode(callSuper = true)
@Data
public class Light extends Device<Light> {

    @Getter(AccessLevel.PRIVATE)
    private final BiConsumer<Integer, Duration> functionToSeBrightness;

    @Getter(AccessLevel.PRIVATE)
    private final Consumer<Boolean> functionToTurnOnOrOff;


    public void setBrightness(int percent, Duration transitionTime) {
        this.getFunctionToSeBrightness().accept(percent, transitionTime);
    }

    public void setOnOrOff(boolean isOn) {
        this.getFunctionToTurnOnOrOff().accept(isOn);
    }

    public String toString() {
        return this.getName();
    }

}
