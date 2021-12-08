package ch.akop.homesystem.models.animation;

import ch.akop.homesystem.models.devices.actor.Light;
import ch.akop.homesystem.services.DeviceService;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@Data
@ConfigurationProperties(prefix = "home-automation.when-main-door-opened")
public class AnimationFactory {

    public final DeviceService deviceService;
    private Duration delayBetween;
    private Duration transitionTime;
    private BigDecimal brightness;
    private Map<Integer, List<String>> turnOn;
    private Set<String> configuredLights;


    @PostConstruct
    public void initializeConfiguredLights() {
        this.configuredLights = turnOn.values()
                .stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
    }

    public Animation buildMainDoorAnimation() {
        var steps = IntStream.range(0, this.turnOn.size())
                .boxed()
                .map(i -> this.turnOn.get(i + 1))
                .map(lightNames -> deviceService.getAllDevices().stream()
                        .filter(device -> lightNames.contains(device.getName()))
                        .map(Light.class::cast)
                        .toList())
                .map(devices -> {
                    final AnimationStep step;
                    if (devices.size() > 1) {
                        step = new ParallelStep(devices.stream()
                                .map(this::createLightStep)
                                .collect(Collectors.toList()));
                    } else {
                        step = createLightStep(devices.get(0));
                    }

                    return List.of(step, new PauseStep(delayBetween));
                })
                .toList();

        return new Animation()
                .setAnimationSteps(steps.stream()
                        .flatMap(Collection::stream)
                        .toList());
    }


    private SetLightStep createLightStep(Light light) {
        return new SetLightStep(light)
                .setTransitionTime(this.transitionTime)
                .setBrightness(this.brightness);
    }

}
