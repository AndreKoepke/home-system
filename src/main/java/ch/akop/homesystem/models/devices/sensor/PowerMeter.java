package ch.akop.homesystem.models.devices.sensor;

import ch.akop.homesystem.models.devices.Device;
import io.reactivex.rxjava3.subjects.ReplaySubject;
import io.reactivex.rxjava3.subjects.Subject;
import lombok.Data;
import lombok.EqualsAndHashCode;


@Data
@EqualsAndHashCode(callSuper = true)
public class PowerMeter extends Device<PowerMeter> {

    private final Subject<Integer> power$ = ReplaySubject.createWithSize(1);
    private final Subject<Integer> current$ = ReplaySubject.createWithSize(1);
    private final Subject<Integer> voltage$ = ReplaySubject.createWithSize(1);

}
