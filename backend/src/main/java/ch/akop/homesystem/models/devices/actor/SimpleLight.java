package ch.akop.homesystem.models.devices.actor;

import ch.akop.homesystem.deconz.rest.State;
import io.reactivex.rxjava3.subjects.ReplaySubject;
import io.reactivex.rxjava3.subjects.Subject;
import java.util.function.Consumer;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@ToString(callSuper = true)
public class SimpleLight extends Actor<SimpleLight> {

  @Getter(AccessLevel.PROTECTED)
  @Setter(AccessLevel.PROTECTED)
  @ToString.Exclude
  private Consumer<Boolean> functionToTurnOnOrOff;

  private Subject<Boolean> state$ = ReplaySubject.createWithSize(1);

  private boolean currentStateIsOn;

  public SimpleLight(Consumer<Boolean> functionToTurnOnOrOff) {
    this.functionToTurnOnOrOff = functionToTurnOnOrOff;
  }

  public void turnOn() {
    this.getFunctionToTurnOnOrOff().accept(true);
  }

  public void turnOff() {
    this.getFunctionToTurnOnOrOff().accept(false);
  }

  public void turnTo(boolean nextState) {
    this.getFunctionToTurnOnOrOff().accept(nextState);
  }

  @Override
  protected void consumeInternalUpdate(State update) {
    currentStateIsOn = update.getOn();
    state$.onNext(currentStateIsOn);
  }
}
