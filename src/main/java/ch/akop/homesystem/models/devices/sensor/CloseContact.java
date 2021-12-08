package ch.akop.homesystem.models.devices.sensor;

import ch.akop.homesystem.models.devices.Device;
import io.reactivex.rxjava3.subjects.ReplaySubject;
import io.reactivex.rxjava3.subjects.Subject;
import lombok.Data;
import lombok.EqualsAndHashCode;

import static ch.akop.homesystem.models.devices.sensor.CloseContactState.*;

@Data
@EqualsAndHashCode(callSuper = true)
public class CloseContact extends Device<CloseContact> {

    private final Subject<CloseContactState> state$ = ReplaySubject.createWithSize(1);
    private CloseContactState state;

    public CloseContact setState(CloseContactState newState) {
        this.state = newState;
        this.state$.onNext(newState);
        return this;
    }

    public CloseContact setOpen(boolean isOpen) {
        return this.setState(isOpen ? OPENED : CLOSED);
    }

}
