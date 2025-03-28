package ch.akop.homesystem.models.devices.sensor;

import ch.akop.homesystem.deconz.rest.State;
import io.reactivex.rxjava3.subjects.ReplaySubject;
import io.reactivex.rxjava3.subjects.Subject;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public class MotionSensor extends Sensor<MotionSensor> {

  private final Subject<Boolean> isMoving$ = ReplaySubject.createWithSize(1);
  private final Subject<Boolean> isDark$ = ReplaySubject.createWithSize(1);
  private final Subject<Integer> targetDistance$ = ReplaySubject.createWithSize(1);
  private final boolean offersDistance;

  private boolean moving;
  private boolean dark;
  private Integer targetDistance;
  private LocalDateTime movingChangedAt;

  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  private LightLevel lightLevel;

  @Override
  protected void consumeInternalUpdate(State update) {
    setMoving(update.getPresence());
    setDark(update.getDark() != null && update.getDark());
    setTargetDistance(targetDistance);
  }

  private void setMoving(boolean moving) {
    if (moving == this.moving) {
      return;
    }
    this.movingChangedAt = LocalDateTime.now();
    this.moving = moving;
    isMoving$.onNext(moving);
  }

  private void setDark(boolean dark) {
    isDark$.onNext(dark);
    this.dark = dark;
  }

  private void setTargetDistance(Integer targetDistance) {
    this.targetDistance = targetDistance;
    if (targetDistance != null) {
      targetDistance$.onNext(targetDistance);
    }
  }
}
