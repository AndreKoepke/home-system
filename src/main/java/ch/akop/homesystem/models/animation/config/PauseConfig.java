package ch.akop.homesystem.models.animation.config;

import ch.akop.homesystem.models.animation.steps.PauseStep;
import lombok.Data;

import java.time.Duration;

@Data
public class PauseConfig {

    private Duration waitFor;


    public PauseStep toPauseStep() {
        return new PauseStep(this.waitFor);
    }

}
