package ch.akop.homesystem.models.devices.actor;

import ch.akop.homesystem.deconz.rest.State;
import io.reactivex.rxjava3.subjects.ReplaySubject;
import io.reactivex.rxjava3.subjects.Subject;
import java.util.function.Consumer;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@SuppressWarnings("UnusedReturnValue")
@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class RollerShutter extends Actor<RollerShutter> {

  private Subject<Integer> liftWasChanged = ReplaySubject.createWithSize(1);
  private Subject<Integer> tiltWasChanged = ReplaySubject.createWithSize(1);
  private Subject<Boolean> openWasChanged = ReplaySubject.createWithSize(1);

  private final Consumer<Integer> functionToSetLift;
  private final Consumer<Integer> functionToSetTilt;
  private final Runnable functionToStep;

  /**
   * 100% means, it is open 0% means, it is closed
   */
  @Min(0)
  @Max(100)
  private Integer currentLift;

  /**
   * Tilt angle
   */
  @Min(0)
  @Max(100)
  private Integer currentTilt;

  private boolean isOpen;

  private RollerShutter setCurrentLift(Integer newValue) {
    liftWasChanged.onNext(newValue);
    currentLift = newValue;
    return this;
  }

  private RollerShutter setIsOpen(Boolean newValue) {
    openWasChanged.onNext(newValue);
    isOpen = newValue;
    return this;
  }

  private RollerShutter setCurrentTilt(Integer newValue) {
    tiltWasChanged.onNext(newValue);
    currentTilt = newValue;
    return this;
  }


  /**
   * Set "tilt" and wait until both reached their position. After that, set lift to the given value.
   *
   * @param lift new lift value
   * @param tilt new tilt value
   */
  public void setLiftAndThenTilt(@Min(0) @Max(100) Integer lift, @Min(0) @Max(100) Integer tilt) {
    log.info("Setting tilt (to " + tilt + ") and lift (to " + lift + ") of " + getName());
    functionToSetTilt.accept(tilt);

    //noinspection ResultOfMethodCallIgnored
    tiltWasChanged
        .filter(newLift -> Math.abs(newLift - tilt) < 20)
        .take(1)
        .subscribe(ignored -> {
          log.info("tilt is ok, setting tilt to " + lift);
          functionToSetLift.accept(lift);
        });
  }

  /**
   * Opens the rollerShutters to maximum value
   */
  public void open() {
    setLiftAndThenTilt(100, 100);
  }

  /**
   * Coles the rollerShutters to minimum value
   */
  public void close() {
    setLiftAndThenTilt(0, 0);
  }

  public boolean isNotCompletelyClosed() {
    return currentLift > 5;
  }


  @Override
  protected void consumeInternalUpdate(State update) {
    setCurrentLift(update.getLift());
    setCurrentTilt(update.getTilt());
    setIsOpen(update.getOpen());
  }
}
