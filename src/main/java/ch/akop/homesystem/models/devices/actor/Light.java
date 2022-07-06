package ch.akop.homesystem.models.devices.actor;

import ch.akop.homesystem.models.devices.Device;
import io.reactivex.rxjava3.subjects.ReplaySubject;
import io.reactivex.rxjava3.subjects.Subject;
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

    private final Subject<Boolean> state$ = ReplaySubject.createWithSize(1);

    private boolean on = false;

    public Light setOn(boolean newValue) {
        on = newValue;
        state$.onNext(newValue);

        return this;
    }

    public void setBrightness(final int percent, final Duration transitionTime) {
        this.getFunctionToSeBrightness().accept(percent, transitionTime);
    }
}
