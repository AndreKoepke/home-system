package ch.akop.homesystem.models.devices.sensor;

import static ch.akop.homesystem.util.Comparer.is;

import ch.akop.homesystem.deconz.rest.State;
import io.reactivex.rxjava3.subjects.ReplaySubject;
import io.reactivex.rxjava3.subjects.Subject;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public class Button extends Sensor<Button> {

  @Getter
  private final Subject<Integer> events$ = ReplaySubject.createWithSize(1);

  @Override
  protected void consumeInternalUpdate(State update) {
    try {
      var lastUpdated = LocalDateTime.parse(update.getLastupdated());
      var updatedBefore = Duration.between(lastUpdated.atZone(ZoneOffset.UTC), ZonedDateTime.now()).abs();
      if (is(updatedBefore).biggerAs(Duration.ofSeconds(30))) {
        events$.onNext(update.getButtonevent());
      }
    } catch (DateTimeParseException e) {
      log.warn("Button got a update with an invalid timestamp. LastUpdated was '"
          + update.getLastupdated() + "' for button '" + this.getName() + "' (id: '" + this.getId() + "')");
    }
  }
}
