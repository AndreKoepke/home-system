package ch.akop.homesystem.models.devices.actor;

import ch.akop.homesystem.models.devices.Device;
import io.reactivex.rxjava3.subjects.ReplaySubject;
import io.reactivex.rxjava3.subjects.Subject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class RollerShutter extends Device<RollerShutter> {

    private Subject<Integer> liftWasChanged = ReplaySubject.createWithSize(1);
    private Subject<Integer> tiltWasChanged = ReplaySubject.createWithSize(1);

    private final Consumer<Integer> functionToSetLift;
    private final Consumer<Integer> functionToSetTilt;
    private final Runnable functionToStep;

    /**
     * 100% means, it is open
     * 0% means, it is closed
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
     * @return CompletitionStage, which will be finished after both values are successfully set.
     */
    public CompletionStage<Void> setLiftAndThenTilt(@Min(0) @Max(100) Integer lift, @Min(0) @Max(100) Integer tilt) {
        functionToSetLift.accept(lift);

        return liftWasChanged
                .filter(lift::equals)
                .doOnNext(ignored -> functionToSetTilt.accept(tilt))
                .switchMap(ignored -> tiltWasChanged)
                .filter(tilt::equals)
                .singleElement()
                .toCompletionStage()
                .thenAccept(ignored -> {
                });
    }

}
