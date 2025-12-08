package ch.akop.homesystem.persistence.model.animation.steps;

import ch.akop.homesystem.persistence.model.animation.Animation;
import ch.akop.homesystem.services.impl.DeviceService;
import ch.akop.homesystem.util.SleepUtil;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.LocalTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@Entity
@Table(name = "animation_step_pause")
@Getter
@Setter
public class PauseStep implements Step {

  @Id
  @GeneratedValue
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
