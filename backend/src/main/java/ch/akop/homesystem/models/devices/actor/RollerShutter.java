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

  public RollerShutter setCurrentLift(Integer newValue) {
    liftWasChanged.onNext(newValue);
    currentLift = newValue;
    return this;
  }

  /**
   * The deconz-connector currently doesn't offer any tilt updates.
   *
   * @return The angle of the shutters
   * @deprecated because current deconz does not updating these values
   */
  @Deprecated(since = "not know yet")
  public RollerShutter setCurrentTilt(Integer newValue) {
    tiltWasChanged.onNext(newValue);
    currentTilt = newValue;
    return this;
  }


  /**
   * Set "lift" and wait until both reached their position. After that, set tilt to the given value.
   *
   * @param lift new lift value
   * @param tilt new tilt value
   */
  public void setLiftAndThenTilt(@Min(0) @Max(100) Integer lift, @Min(0) @Max(100) Integer tilt) {
    log.info("Setting lift (to {}) and tilt (to {}) of {}", lift, tilt, getName());
    functionToSetLift.accept(lift);

    //noinspection ResultOfMethodCallIgnored
    liftWasChanged
        .filter(newLift -> Math.abs(newLift - lift) < 20)
        .take(1)
        .subscribe(ignored -> {
          log.info("lift is ok, setting tilt to {}", tilt);
          functionToSetTilt.accept(tilt);
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

  public boolean isCurrentlyOpen() {
    // normally 255 means, that is open. But the actor never reaches 255.
    return currentLift > 240;
  }

  @Override
  protected void consumeInternalUpdate(State update) {
    // bri as workaround, tilt was never updated
    setCurrentLift(update.getBri());
  }
}
