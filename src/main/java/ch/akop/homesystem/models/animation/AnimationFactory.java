package ch.akop.homesystem.models.animation;

import ch.akop.homesystem.models.animation.config.AnimationConfig;
import ch.akop.homesystem.models.devices.actor.Light;
import ch.akop.homesystem.services.DeviceService;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;

@Service
@Data
@ConfigurationProperties(prefix = "home-automation.when-main-door-opened")
public class AnimationFactory {

    private final DeviceService deviceService;

    private List<AnimationConfig> animation;


    public Animation buildMainDoorAnimation() {
        final var allLights = new HashSet<>(this.deviceService.getDevicesOfType(Light.class));

        return new Animation().setAnimationSteps(this.animation.stream()
                .map(config -> config.toAnimationStep(allLights))
                .toList());

    }

}
