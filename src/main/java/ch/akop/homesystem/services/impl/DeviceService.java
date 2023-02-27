package ch.akop.homesystem.services.impl;

import ch.akop.homesystem.models.devices.Device;
import ch.akop.homesystem.models.devices.actor.DimmableLight;
import ch.akop.homesystem.models.devices.actor.SimpleLight;
import ch.akop.homesystem.models.devices.other.Group;
import ch.akop.homesystem.models.devices.other.Scene;
import ch.akop.homesystem.persistence.model.animation.Animation;
import ch.akop.homesystem.persistence.model.config.BasicConfig;
import ch.akop.homesystem.persistence.repository.config.AnimationRepository;
import ch.akop.homesystem.persistence.repository.config.BasicConfigRepository;
import ch.akop.homesystem.util.SleepUtil;
import io.quarkus.vertx.ConsumeEvent;
import io.vertx.core.impl.ConcurrentHashSet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.time.temporal.ChronoUnit.MILLIS;
import static java.time.temporal.ChronoUnit.SECONDS;


@RequiredArgsConstructor
@ApplicationScoped
@Slf4j
public class DeviceService {

    private final List<Device<?>> devices = new ArrayList<>();
    private final Set<Animation> runningAnimations = new ConcurrentHashSet<>();
    private final BasicConfigRepository basicConfigRepository;
    private final AnimationRepository animationRepository;


    public <T extends Device<?>> Optional<T> findDeviceByName(String name, Class<T> clazz) {
        return getDevicesOfType(clazz)
                .stream()
                .filter(device -> device.getName().equalsIgnoreCase(name))
                .findFirst();
    }

    public <T extends Device<?>> Optional<T> findDeviceById(String name, Class<T> clazz) {
        return getDevicesOfType(clazz)
                .stream()
                .filter(device -> device.getId().equals(name))
                .findFirst();
    }

    public <T extends Device<?>> void registerDevice(T device) {
        devices.add(device);
    }


    public Collection<Device<?>> getAllDevices() {
        return new ArrayList<>(devices);
    }


    public <T> Collection<T> getDevicesOfType(Class<T> clazz) {
        return devices.stream()
                .filter(clazz::isInstance)
                .map(clazz::cast)
                .collect(Collectors.toSet());
    }

    @Transactional
    public void turnAllLightsOff() {
        var notLights = basicConfigRepository.findFirstByOrderByModifiedDesc()
                .map(BasicConfig::getNotLights)
                .orElse(new HashSet<>());

        getDevicesOfType(SimpleLight.class).stream()
                .filter(light -> !notLights.contains(light.getName()))
                .filter(Device::isReachable)
                .forEach(light -> {
                    // see #74, if the commands are cumming to fast, then maybe lights are not correctly off
                    // if this workaround helps, then this should be removed for a rate-limit (see #3)
                    SleepUtil.sleep(Duration.of(25, MILLIS));

                    if (light instanceof DimmableLight dimmable) {
                        dimmable.setBrightness(0, Duration.of(10, SECONDS));
                    } else {
                        light.turnOn(false);
                    }
                });
    }

    @Transactional
    public boolean isAnyLightOn() {
        var notLights = basicConfigRepository.findFirstByOrderByModifiedDesc()
                .map(BasicConfig::getNotLights)
                .orElse(new HashSet<>());

        return getDevicesOfType(SimpleLight.class)
                .stream()
                .filter(Device::isReachable)
                .filter(light -> !notLights.contains(light.getName()))
                .anyMatch(SimpleLight::isCurrentStateIsOn);
    }

    @Transactional
    @ConsumeEvent(value = "home/playAnimation", blocking = true)
    public void event(Animation animation) {
        if (runningAnimations.contains(animation)) {
            return;
        }

        runningAnimations.add(animation);
        var freshAnimation = animationRepository.getOne(animation.getId());
        freshAnimation.play(this);
        runningAnimations.remove(animation);
    }

    public void activeSceneForAllGroups(String sceneName) {
        getDevicesOfType(Group.class)
                .stream()
                .flatMap(group -> group.getScenes().stream())
                .filter(scene -> scene.getName().equals(sceneName))
                .forEach(Scene::activate);
    }
}
