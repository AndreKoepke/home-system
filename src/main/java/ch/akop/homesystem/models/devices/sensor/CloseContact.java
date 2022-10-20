package ch.akop.homesystem.models.devices.sensor;

import ch.akop.homesystem.deconz.rest.State;
import io.reactivex.rxjava3.subjects.ReplaySubject;
import io.reactivex.rxjava3.subjects.Subject;
import lombok.Data;
import lombok.EqualsAndHashCode;

import static ch.akop.homesystem.models.devices.sensor.CloseContactState.CLOSED;
import static ch.akop.homesystem.models.devices.sensor.CloseContactState.OPENED;

@Data
@EqualsAndHashCode(callSuper = true)
public class CloseContact extends Sensor<CloseContact> {

    private final Subject<CloseContactState> state$ = ReplaySubject.createWithSize(1);
    private CloseContactState state;

    @Override
    public CloseContact consumeUpdate(State update) {
        setState(Boolean.TRUE.equals(update.getOpen()) ? OPENED : CLOSED);
        state$.onNext(getState());

        return this;
    }
}
