package ch.akop.homesystem.persistence.model.animation.steps;

import ch.akop.homesystem.util.SleepUtil;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.time.Duration;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "animation_step_pause")
@Getter
@Setter
public class PauseStep implements Step {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NonNull
    private Integer sortOrder;

    @NonNull
    private Duration waitFor;

    @Override
    public void play() {
        var started = LocalTime.now();
        try {
            Thread.sleep(waitFor.toMillis());
        } catch (InterruptedException ignored) {
            // don't interrupt animation
            // they are very short, so we can allow them to finish
            var waitingTimeLeft = Duration.between(started, LocalTime.now()).abs();
            SleepUtil.sleep(waitingTimeLeft);
        }
    }
}
