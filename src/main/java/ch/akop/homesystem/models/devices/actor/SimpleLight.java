package ch.akop.homesystem.models.devices.actor;

import ch.akop.homesystem.models.devices.Device;
import io.reactivex.rxjava3.subjects.ReplaySubject;
import io.reactivex.rxjava3.subjects.Subject;
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

    private Subject<Boolean> state$ = ReplaySubject.createWithSize(1);

    private boolean currentState;

    public SimpleLight(Consumer<Boolean> functionToTurnOnOrOff) {
        this.functionToTurnOnOrOff = functionToTurnOnOrOff;
    }

    public SimpleLight updateState(boolean isOn) {
        this.currentState = isOn;
        this.state$.onNext(isOn);
        return this;
    }

    public void turnOn(boolean on) {
        this.getFunctionToTurnOnOrOff().accept(on);
    }
}
