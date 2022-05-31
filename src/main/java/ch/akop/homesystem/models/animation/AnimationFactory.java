package ch.akop.homesystem.models.animation;

import ch.akop.homesystem.models.animation.config.AnimationConfig;
import ch.akop.homesystem.models.devices.actor.SimpleLight;
import ch.akop.homesystem.services.DeviceService;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Data
@ConfigurationProperties(prefix = "home-automation.when-main-door-opened")
public class AnimationFactory {

    private final DeviceService deviceService;

    private List<AnimationConfig> animation;


    public Animation buildMainDoorAnimation() {
        var allLightNamesOfAnimation = animation.stream()
                .flatMap(animationConfig -> animationConfig.getLights().getNames().stream())
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        var lightsByName = this.deviceService.getDevicesOfType(SimpleLight.class).stream()
                .filter(light -> allLightNamesOfAnimation.contains(light.getName().toLowerCase()))
                .collect(Collectors.toMap(
                        light -> light.getName().toLowerCase(),
                        light -> light
                ));

        return new Animation().setAnimationSteps(this.animation.stream()
                .map(config -> config.toAnimationStep(config.getLights().getNames().stream()
                        .map(String::toLowerCase)
                        .map(lightsByName::get)
                        .collect(Collectors.toSet())))
                .toList());

    }

}
