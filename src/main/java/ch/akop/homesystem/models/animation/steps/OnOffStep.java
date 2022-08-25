package ch.akop.homesystem.models.animation.steps;


import ch.akop.homesystem.models.devices.actor.SimpleLight;
import lombok.Data;
import lombok.NonNull;

@Data
public class OnOffStep implements AnimationStep {

    @NonNull
    private final SimpleLight light;
    private boolean turnLightOn;

    @Override
    public void play() {
        light.turnOn(turnLightOn);
    }
}
