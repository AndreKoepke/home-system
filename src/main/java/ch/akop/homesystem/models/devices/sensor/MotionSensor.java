package ch.akop.homesystem.models.devices.sensor;

import ch.akop.homesystem.models.devices.Device;
import io.reactivex.rxjava3.subjects.ReplaySubject;
import io.reactivex.rxjava3.subjects.Subject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
public class MotionSensor extends Device<MotionSensor> {

    @Getter
    private final Subject<Boolean> isMoving$ = ReplaySubject.createWithSize(1);

    @Getter
    private final Subject<Boolean> isDark$ = ReplaySubject.createWithSize(1);

    @Getter
    private LocalDateTime lastMovement = LocalDateTime.MIN;

    public void updateState(final boolean movement, final boolean dark) {
        super.setLastChange(LocalDateTime.now());
        this.isMoving$.onNext(movement);
        this.isDark$.onNext(dark);
    }


}
