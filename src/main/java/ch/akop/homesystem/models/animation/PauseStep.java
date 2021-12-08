package ch.akop.homesystem.models.animation;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;

import java.time.Duration;

@Data
@EqualsAndHashCode(callSuper = false)
public class PauseStep extends  AnimationStep {

    private final Duration time;


    @Override
    @SneakyThrows
    void play() {
        Thread.sleep(time.toMillis());
    }
}
