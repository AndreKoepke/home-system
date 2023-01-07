package ch.akop.homesystem.services.impl;

import ch.akop.homesystem.models.devices.Device;
import ch.akop.homesystem.models.devices.actor.DimmableLight;
import ch.akop.homesystem.models.devices.actor.SimpleLight;
import ch.akop.homesystem.models.devices.other.Group;
import ch.akop.homesystem.models.devices.other.Scene;
import ch.akop.homesystem.persistence.model.config.BasicConfig;
import ch.akop.homesystem.persistence.repository.config.BasicConfigRepository;
import ch.akop.homesystem.services.DeviceService;
import ch.akop.homesystem.util.SleepUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.time.temporal.ChronoUnit.MILLIS;
import static java.time.temporal.ChronoUnit.SECONDS;


@RequiredArgsConstructor
@Service
@Slf4j
public class DeviceServiceImpl implements DeviceService {

    private final List<Device<?>> devices = new ArrayList<>();
    private final BasicConfigRepository basicConfigRepository;


    @Override
    public <T extends Device<?>> Optional<T> findDeviceByName(String name, Class<T> clazz) {
        return getDevicesOfType(clazz)
                .stream()
                .filter(device -> device.getName().equalsIgnoreCase(name))
                .findFirst();
    }

    @Override
    public <T extends Device<?>> Optional<T> findDeviceById(String name, Class<T> clazz) {
        return getDevicesOfType(clazz)
                .stream()
                .filter(device -> device.getId().equals(name))
                .findFirst();
    }

    @Override
    public <T extends Device<?>> void registerDevice(T device) {
        devices.add(device);
    }

    @Override
    public Collection<Device<?>> getAllDevices() {
        return new ArrayList<>(devices);
    }

    @Override
    public <T> Collection<T> getDevicesOfType(Class<T> clazz) {
        return devices.stream()
                .filter(clazz::isInstance)
                .map(clazz::cast)
                .collect(Collectors.toSet());
    }

    @Override
    @Transactional
    public void turnAllLightsOff() {
        var notLights = basicConfigRepository.findFirstByOrderByModifiedDesc()
                .map(BasicConfig::getNotLights)
                .orElse(new HashSet<>());

        getDevicesOfType(SimpleLight.class).stream()
                .filter(light -> !notLights.contains(light.getName()))
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

    @Override
    @Transactional
    public boolean isAnyLightOn() {
        var notLights = basicConfigRepository.findFirstByOrderByModifiedDesc()
                .map(BasicConfig::getNotLights)
                .orElse(new HashSet<>());

        return getDevicesOfType(SimpleLight.class)
                .stream()
                .filter(light -> !notLights.contains(light.getName()))
                .anyMatch(SimpleLight::isCurrentStateIsOn);
    }

    @Override
    public void activeSceneForAllGroups(String sceneName) {
        getDevicesOfType(Group.class)
                .stream()
                .flatMap(group -> group.getScenes().stream())
                .filter(scene -> scene.getName().equals(sceneName))
                .forEach(Scene::activate);
    }
}
