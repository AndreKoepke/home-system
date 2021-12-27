package ch.akop.homesystem.models.animation.steps;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;

import java.time.Duration;

@Data
@EqualsAndHashCode(callSuper = false)
public class PauseStep implements AnimationStep {

    private final Duration time;


    @Override
    @SneakyThrows
    public void play() {
        Thread.sleep(this.time.toMillis());
    }
}
