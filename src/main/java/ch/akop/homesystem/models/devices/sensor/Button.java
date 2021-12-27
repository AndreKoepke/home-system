package ch.akop.homesystem.models.devices.sensor;

import ch.akop.homesystem.models.devices.Device;
import io.reactivex.rxjava3.subjects.ReplaySubject;
import io.reactivex.rxjava3.subjects.Subject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Data
@EqualsAndHashCode(callSuper = true)
public class Button extends Device<Button> {

    @Getter
    private final Subject<Integer> events$ = ReplaySubject.createWithSize(1);

    public void triggerEvent(final int event) {
        this.events$.onNext(event);
    }

}
