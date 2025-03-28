package ch.akop.homesystem.persistence.model.animation.steps;

import ch.akop.homesystem.persistence.model.animation.Animation;
import ch.akop.homesystem.services.impl.DeviceService;
import ch.akop.homesystem.util.SleepUtil;
import java.time.Duration;
import java.time.LocalTime;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

@Entity
@Table(name = "animation_step_pause")
@Getter
@Setter
public class PauseStep implements Step {

  @Id
  @GeneratedValue(generator = "UUID")
  @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
  private UUID id;

  @NonNull
  private Integer sortOrder;

  @ManyToOne
  private Animation animation;

  @NonNull
  private Duration waitFor;

  @Override
  public void play(DeviceService deviceService) {
    var started = LocalTime.now();
    try {
      Thread.sleep(waitFor.toMillis());
    } catch (InterruptedException ignored) {
      // don't interrupt animation
      // they are very short, so we can allow them to finish
      var waitingTimeLeft = Duration.between(started, LocalTime.now())
          .abs();
      SleepUtil.sleep(waitingTimeLeft);
    }
  }

  @Override
  public String getNameOfLight() {
    return null;
  }

  @Override
  public String getActionDescription() {
    return "warte " + waitFor.getSeconds() + "s";
  }
}
