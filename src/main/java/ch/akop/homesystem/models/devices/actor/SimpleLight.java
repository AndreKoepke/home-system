package ch.akop.homesystem.models.devices.actor;

import ch.akop.homesystem.models.devices.Device;
import lombok.*;

import java.util.function.Consumer;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@ToString(callSuper = true)
public class SimpleLight extends Device<SimpleLight> {

    @Getter(AccessLevel.PROTECTED)
    @Setter(AccessLevel.PROTECTED)
    @ToString.Exclude
    private Consumer<Boolean> functionToTurnOnOrOff;

    private boolean on;

    public SimpleLight(final Consumer<Boolean> functionToTurnOnOrOff) {
        this.functionToTurnOnOrOff = functionToTurnOnOrOff;
    }

    public void turnOn(final boolean on) {
        this.getFunctionToTurnOnOrOff().accept(on);
    }
}
