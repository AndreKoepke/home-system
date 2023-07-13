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
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

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
  private final TimedGateKeeper automaticActionLock = new TimedGateKeeper();
  private final TimedGateKeeper highWindLock = new TimedGateKeeper();

  private final Consumer<Integer> functionToSetLift;
  private final Consumer<Integer> functionToSetTilt;

  private LocalDateTime lastManuallAction = LocalDateTime.MIN;

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
  public Completable setLiftAndThenTilt(@Min(0) @Max(100) Integer lift, @Min(0) @Max(100) Integer tilt) {

    if (!highWindLock.isGateOpen()) {
      log.warn("Ignored command because of high wind speed for " + getName());
      return Completable.complete();
    }

    return setTiltTo(tilt)
        .andThen(Completable.fromRunnable(() -> {
          if (Math.abs(currentLift - lift) > LIFT_ALLOWED_TOLERANCE) {
            log.info(this.getName() + ": lift (now at " + currentLift + ") is nok, set to " + lift);
            functionToSetLift.accept(lift);
            automaticActionLock.blockFor(Duration.ofMinutes(1));
          }
        }))
        .observeOn(Schedulers.io())
        .subscribeOn(Schedulers.io());
  }

  @NotNull
  private Completable setTiltTo(Integer tilt) {
    if (Math.abs(currentTilt - tilt) < TILT_ALLOWED_DIFFERENCE) {
      return Completable.complete();
    }

    return Completable.fromRunnable(() -> {
          log.info(this.getName() + ": tilt (now at " + currentTilt + ") nok, set to " + tilt);
          functionToSetTilt.accept(tilt);
          automaticActionLock.blockFor(Duration.ofSeconds(10));
        })
        .andThen(tilt$
            .filter(newTilt -> Math.abs(newTilt - tilt) < TILT_ALLOWED_DIFFERENCE)
            .timeout(10, TimeUnit.SECONDS)
            .onErrorResumeNext(throwable -> Observable.just(1))
            .take(1)
            .flatMapCompletable(integer -> Completable.complete()));
  }

  /**
   * Opens the rollerShutters to maximum value
   */
  public Completable open() {
    return setLiftAndThenTilt(100, 100);
  }

  /**
   * Coles the rollerShutters to minimum value
   */
  public Completable close() {
    return setLiftAndThenTilt(0, 0);
  }

  public void reportHighWind() {
    close().subscribe();
    highWindLock.blockFor(BLOCK_TIME_WHEN_HIGH_WIND);
  }

  @Override
  protected void consumeInternalUpdate(State update) {
    if (currentLift != null && automaticActionLock.isGateOpen()) {
      lastManuallAction = LocalDateTime.now();
    }

    setCurrentLift(update.getLift());
    setCurrentTilt(update.getTilt());
    setIsOpen(update.getOpen());
  }

  private void setCurrentLift(Integer newValue) {
    lift$.onNext(newValue);
    currentLift = newValue;
  }

  private void setIsOpen(Boolean newValue) {
    open$.onNext(newValue);
    isOpen = newValue;
  }

  private void setCurrentTilt(Integer newValue) {
    tilt$.onNext(newValue);
    currentTilt = newValue;
  }
}
