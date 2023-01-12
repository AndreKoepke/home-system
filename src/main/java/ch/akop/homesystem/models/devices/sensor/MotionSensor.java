package ch.akop.homesystem.models.devices.sensor;

import ch.akop.homesystem.deconz.rest.State;
import io.reactivex.rxjava3.subjects.ReplaySubject;
import io.reactivex.rxjava3.subjects.Subject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Data
@EqualsAndHashCode(callSuper = true)
public class MotionSensor extends Sensor<MotionSensor> {

    @Getter
    private final Subject<Boolean> isMoving$ = ReplaySubject.createWithSize(1);

    @Getter
    private final Subject<Boolean> isDark$ = ReplaySubject.createWithSize(1);

    @Override
    protected void consumeInternalUpdate(State update) {
        isMoving$.onNext(update.getPresence());
        isDark$.onNext(update.getDark() != null && update.getDark());
    }
}
