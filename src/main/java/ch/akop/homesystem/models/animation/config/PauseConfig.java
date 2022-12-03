package ch.akop.homesystem.models.animation.config;

import ch.akop.homesystem.models.animation.steps.PauseStep;
import lombok.Value;

import java.time.Duration;

@Value
public class PauseConfig {

    Duration waitFor;

    public PauseStep toPauseStep() {
        return new PauseStep(waitFor);
    }
}
