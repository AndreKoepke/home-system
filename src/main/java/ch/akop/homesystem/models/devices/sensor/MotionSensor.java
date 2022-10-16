package ch.akop.homesystem.models.devices.sensor;

import ch.akop.homesystem.deconz.rest.State;
import ch.akop.homesystem.models.devices.Device;
import io.reactivex.rxjava3.subjects.ReplaySubject;
import io.reactivex.rxjava3.subjects.Subject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Data
@EqualsAndHashCode(callSuper = true)
public class MotionSensor extends Device<MotionSensor> {

    @Getter
    private final Subject<Boolean> isMoving$ = ReplaySubject.createWithSize(1);

    @Getter
    private final Subject<Boolean> isDark$ = ReplaySubject.createWithSize(1);

    @Override
    public MotionSensor consumeUpdate(State update) {
        isMoving$.onNext(update.getPresence());
        isDark$.onNext(update.getDark() != null && update.getDark());

        return this;
    }
}
