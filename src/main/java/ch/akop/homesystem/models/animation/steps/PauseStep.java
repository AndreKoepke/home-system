package ch.akop.homesystem.models.animation.steps;

import java.time.Duration;


public record PauseStep(Duration time) implements AnimationStep {

    @Override
    public void play() {
        try {
            Thread.sleep(this.time.toMillis());
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
