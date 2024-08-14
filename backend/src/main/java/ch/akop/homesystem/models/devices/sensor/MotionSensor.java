package ch.akop.homesystem.models.devices.sensor;

import ch.akop.homesystem.deconz.rest.State;
import io.reactivex.rxjava3.subjects.ReplaySubject;
import io.reactivex.rxjava3.subjects.Subject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public class MotionSensor extends Sensor<MotionSensor> {

  private final Subject<Boolean> isMoving$ = ReplaySubject.createWithSize(1);
  private final Subject<Boolean> isDark$ = ReplaySubject.createWithSize(1);
  private final Subject<Integer> targetDistance$ = ReplaySubject.createWithSize(1);
  private final boolean offersDistance;

  private LightLevel lightLevel;

  @Override
  protected void consumeInternalUpdate(State update) {
    isMoving$.onNext(update.getPresence());
    isDark$.onNext(update.getDark() != null && update.getDark());
    if (update.getTargetdistance() != null) {
      targetDistance$.onNext(update.getTargetdistance());
      log.info("targetDistance is {} and there is {}movement", update.getTargetdistance(), update.getPresence() ? "" : "no ");
    }
  }
}
