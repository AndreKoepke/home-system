package ch.akop.homesystem.models.devices.sensor;

import ch.akop.homesystem.deconz.rest.State;
import io.reactivex.rxjava3.subjects.ReplaySubject;
import io.reactivex.rxjava3.subjects.Subject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Data
@EqualsAndHashCode(callSuper = true)
@Slf4j
public class LightLevel extends Sensor<LightLevel> {

  private final Subject<Integer> lux$ = ReplaySubject.createWithSize(1);
  private final Subject<Integer> lightLevel$ = ReplaySubject.createWithSize(1);
  private final Subject<Boolean> dayLight$ = ReplaySubject.createWithSize(1);

  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  private MotionSensor motionSensor;

  @Override
  protected void consumeInternalUpdate(State update) {
    lux$.onNext(update.getLux());
    lightLevel$.onNext(update.getLightlevel());
    dayLight$.onNext(update.getDaylight());

    log.info("Lux is {}", update.getLux());
  }
}
