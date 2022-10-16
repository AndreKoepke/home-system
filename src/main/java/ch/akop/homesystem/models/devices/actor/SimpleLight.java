package ch.akop.homesystem.models.devices.actor;

import ch.akop.homesystem.deconz.rest.State;
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

    private boolean currentStateIsOn;

    public SimpleLight(Consumer<Boolean> functionToTurnOnOrOff) {
        this.functionToTurnOnOrOff = functionToTurnOnOrOff;
    }

    public void turnOn(boolean on) {
        this.getFunctionToTurnOnOrOff().accept(on);
    }

    public boolean isCurrentlyOff() {
        return !this.currentStateIsOn;
    }

    @Override
    public SimpleLight consumeUpdate(State update) {
        currentStateIsOn = update.getOn();
        state$.onNext(update.getOn());

        return this;
    }
}
