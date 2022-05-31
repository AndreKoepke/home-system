package ch.akop.homesystem.models.devices.actor;

import ch.akop.homesystem.models.devices.Device;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.function.Consumer;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class SimpleLight extends Device<SimpleLight> {

    @Getter(AccessLevel.PROTECTED)
    @Setter(AccessLevel.PROTECTED)
    private Consumer<Boolean> functionToTurnOnOrOff;

    private boolean on;

    public SimpleLight(Consumer<Boolean> functionToTurnOnOrOff) {
        this.functionToTurnOnOrOff = functionToTurnOnOrOff;
    }

    public void turnOn(boolean on) {
        this.getFunctionToTurnOnOrOff().accept(on);
    }
}
