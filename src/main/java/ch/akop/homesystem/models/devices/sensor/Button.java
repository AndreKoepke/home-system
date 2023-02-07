package ch.akop.homesystem.models.devices.sensor;

import ch.akop.homesystem.deconz.rest.State;
import io.reactivex.rxjava3.subjects.ReplaySubject;
import io.reactivex.rxjava3.subjects.Subject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
public class Button extends Sensor<Button> {

    @Getter
    private final Subject<Integer> events$ = ReplaySubject.createWithSize(1);

    @Override
    protected void consumeInternalUpdate(State update) {
        var updatedBefore = Duration.between(update.getLastupdated().atZone(ZoneOffset.UTC), ZonedDateTime.now()).abs();
        if (updatedBefore.compareTo(Duration.ofSeconds(30)) < 0) {
            events$.onNext(update.getButtonevent());
        }
    }
}
