package ch.akop.homesystem.models.devices.actor;

import ch.akop.homesystem.deconz.rest.State;
import ch.akop.homesystem.util.TimedGateKeeper;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.ReplaySubject;
import io.reactivex.rxjava3.subjects.Subject;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("UnusedReturnValue")
@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class RollerShutter extends Actor<RollerShutter> {

  public static final int TILT_ALLOWED_DIFFERENCE = 20;
  public static final int LIFT_ALLOWED_TOLERANCE = 5;
  public static final Duration BLOCK_TIME_WHEN_HIGH_WIND = Duration.ofHours(1);

  private final Subject<Integer> lift$ = ReplaySubject.createWithSize(1);
  private final Subject<Integer> tilt$ = ReplaySubject.createWithSize(1);
  private final Subject<Boolean> open$ = ReplaySubject.createWithSize(1);

  @Getter(AccessLevel.PRIVATE)
  private final TimedGateKeeper highWindLock = new TimedGateKeeper();

  @Getter(AccessLevel.PRIVATE)
  private final Consumer<Integer> functionToSetLift;

  @Getter(AccessLevel.PRIVATE)
  private final Consumer<Integer> functionToSetTilt;

  private final boolean hasTiltCapability;

  /**
   * Some rollerShutters are blocking when closing. To avoid that, these rollerShutters can be closed only half and after that, open a bit and close again.
   */
  @Getter(AccessLevel.PRIVATE)
  private final boolean closeWithInterruption;

  @Setter(AccessLevel.PRIVATE)
  private LocalDateTime lastManuallAction = LocalDateTime.MIN;

  @Getter(AccessLevel.PRIVATE)
  @Setter(AccessLevel.PRIVATE)
  private Integer automaticTiltTarget = null;

  @Getter(AccessLevel.PRIVATE)
  @Setter(AccessLevel.PRIVATE)
  private Integer automaticLiftTarget = null;

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

  /**
   * Set "tilt" and wait until both reached their position. After that, set lift to the given value.
   *
   * @param lift new lift value
   * @param tilt new tilt value
   */
  public Completable setLiftAndThenTilt(@Min(0) @Max(100) Integer lift, @Min(0) @Max(100) Integer tilt, String reason) {

    if (!highWindLock.isGateOpen()) {
      log.warn("Ignored command because of high wind speed for {}", getName());
      return Completable.complete();
    }

    return setTiltTo(tilt, reason)
        .andThen(Completable.fromRunnable(() -> {
          if (Math.abs(currentLift - lift) > LIFT_ALLOWED_TOLERANCE) {
            log.info("{}: lift (now at {}) is nok, set to {} because of {}", getName(), currentLift, lift, reason);
            automaticLiftTarget = lift;
            functionToSetLift.accept(lift);
          }
        }))
        .doFinally(() -> automaticLiftTarget = null);
  }

  @NotNull
  private Completable setTiltTo(Integer tilt, String reason) {
    if (!hasTiltCapability || Math.abs(currentTilt - tilt) < TILT_ALLOWED_DIFFERENCE) {
      return Completable.complete();
    }

    return Completable.fromRunnable(() -> {
          log.info("{}: tilt (now at {}) nok, set to {} because of {}", this.getName(), currentTilt, tilt, reason);
          automaticTiltTarget = tilt;
          functionToSetTilt.accept(tilt);
        })
        .andThen(tilt$
            .observeOn(Schedulers.io())
            .filter(newTilt -> Math.abs(newTilt - tilt) < TILT_ALLOWED_DIFFERENCE)
            .timeout(10, TimeUnit.SECONDS, Schedulers.io())
            .onErrorResumeNext(throwable -> Observable.just(1))
            .take(1)
            .ignoreElements())
        .doFinally(() -> automaticTiltTarget = null);
  }

  /**
   * Opens the rollerShutters to maximum value
   */
  public Completable open(String reason) {
    return setLiftAndThenTilt(100, 100, reason);
  }

  /**
   * Closes the rollerShutters to minimum value
   */
  public Completable close(String reason) {
    if (closeWithInterruption && currentLift > 60) {
      return setLiftAndThenTilt(50, 100, reason)
          .delay(5, TimeUnit.SECONDS)
          .andThen(setLiftAndThenTilt(0, 0, reason + " after interruption"));
    }
    return setLiftAndThenTilt(0, 0, reason);
  }

  public void reportHighWind() {
    open("high wind").subscribe();
    highWindLock.blockFor(BLOCK_TIME_WHEN_HIGH_WIND);
  }

  @Override
  protected void consumeInternalUpdate(State update) {
    if (isUpdateCausedByManualCommand(automaticLiftTarget, currentLift, update.getLift())
        || isUpdateCausedByManualCommand(automaticTiltTarget, currentTilt, update.getTilt())) {
      lastManuallAction = LocalDateTime.now();
    }

    setCurrentLift(update.getLift());
    setCurrentTilt(update.getTilt());
    setIsOpen(update.getOpen());
  }

  boolean isUpdateCausedByManualCommand(Integer targetValue, Integer previousValue, Integer updateValue) {
    if (previousValue == null) {
      return false;
    }

    if (targetValue == null) {
      return true;
    }

    var differenceBefore = Math.abs(previousValue - targetValue);
    var differenceAfter = Math.abs(updateValue - targetValue);

    return differenceAfter > differenceBefore;
  }

  private void setCurrentLift(Integer newValue) {
    lift$.onNext(newValue);
    currentLift = newValue;
  }

  private void setIsOpen(Boolean newValue) {
    open$.onNext(newValue);
    isOpen = newValue;
  }

  private void setCurrentTilt(@Nullable Integer newValue) {
    if (!hasTiltCapability || newValue == null) {
      return;
    }
    tilt$.onNext(newValue);
    currentTilt = newValue;
  }
}
