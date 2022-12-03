package ch.akop.homesystem.models.animation;

import ch.akop.homesystem.config.properties.HomeSystemProperties;
import ch.akop.homesystem.models.devices.actor.SimpleLight;
import ch.akop.homesystem.services.DeviceService;
import lombok.Data;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Data
public class AnimationFactory {

    private final DeviceService deviceService;

    private final HomeSystemProperties homeSystemProperties;


    public Animation buildMainDoorAnimation() {
        var allLightNamesOfAnimation = homeSystemProperties.getWhenMainDoorOpened().stream()
                .filter(animationConfig -> animationConfig.getLights() != null)
                .flatMap(animationConfig -> animationConfig.getLights().getNames().stream())
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        var lightsByName = deviceService.getDevicesOfType(SimpleLight.class).stream()
                .filter(light -> allLightNamesOfAnimation.contains(light.getName().toLowerCase()))
                .collect(Collectors.toMap(
                        light -> light.getName().toLowerCase(),
                        light -> light
                ));

        return new Animation().setAnimationSteps(homeSystemProperties.getWhenMainDoorOpened().stream()
                .map(config -> config.toAnimationStep(config.getLights() == null ? new HashSet<>() : config.getLights().getNames().stream()
                        .map(String::toLowerCase)
                        .map(lightsByName::get)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet())))
                .toList());

    }

}
