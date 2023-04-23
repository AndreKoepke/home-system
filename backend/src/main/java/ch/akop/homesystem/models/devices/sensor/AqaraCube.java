package ch.akop.homesystem.models.devices.sensor;

import ch.akop.homesystem.deconz.rest.State;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.ReplaySubject;
import io.reactivex.rxjava3.subjects.Subject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public class AqaraCube extends Sensor<AqaraCube> {

  @Getter
  private final Subject<Integer> activeSide$ = ReplaySubject.createWithSize(1);

  private final Subject<EMPTY> shacked$ = PublishSubject.create();

  public enum EMPTY {INSTANCE}

  @Override
  protected void consumeInternalUpdate(State update) {
    if (update.getButtonevent() != null) {
      if (update.getButtonevent() < 7000) {
        var firstNumber = update.getButtonevent() / 1000;
        activeSide$.onNext(firstNumber);
      } else if (update.getButtonevent() == 7007) {
        shacked$.onNext(EMPTY.INSTANCE);
      }
    }
  }

}
