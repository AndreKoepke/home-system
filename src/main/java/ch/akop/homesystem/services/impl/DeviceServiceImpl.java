package ch.akop.homesystem.services.impl;

import ch.akop.homesystem.config.properties.HomeSystemProperties;
import ch.akop.homesystem.models.devices.Device;
import ch.akop.homesystem.models.devices.actor.DimmableLight;
import ch.akop.homesystem.models.devices.actor.SimpleLight;
import ch.akop.homesystem.services.DeviceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@RequiredArgsConstructor
@Service
@Slf4j
public class DeviceServiceImpl implements DeviceService {

    private final List<Device<?>> devices = new ArrayList<>();
    private final HomeSystemProperties homeSystemProperties;

    public <T extends Device<?>> T getDeviceById(final String id, final Class<T> clazz) {
        return this.getDevicesOfType(clazz)
                .stream()
                .filter(device -> device.getId().equals(id))
                .findFirst()
                .orElseThrow();
    }

    @Override
    public <T extends Device<?>> Optional<T> findDeviceByName(String name, Class<T> clazz) {
        return this.getDevicesOfType(clazz)
                .stream()
                .filter(device -> device.getName().equalsIgnoreCase(name))
                .findFirst();
    }

    @Override
    public <T extends Device<?>> void registerDevice(final T device) {
        this.devices.add(device);
    }

    @Override
    public Collection<Device<?>> getAllDevices() {
        return new ArrayList<>(this.devices);
    }

    @Override
    public <T> Collection<T> getDevicesOfType(final Class<T> clazz) {
        return this.devices.stream()
                .filter(clazz::isInstance)
                .map(clazz::cast)
                .collect(Collectors.toSet());
    }

    @Override
    public void turnAllLightsOff() {
        this.getDevicesOfType(SimpleLight.class).stream()
                .filter(light -> !homeSystemProperties.getNotLights().contains(light.getName()))
                .forEach(light -> {
                    if (light instanceof DimmableLight dimmable) {
                        dimmable.setBrightness(0, Duration.of(10, ChronoUnit.SECONDS));
                    } else {
                        light.turnOn(false);
                    }
                });
    }

    @Override
    public boolean isAnyLightOn() {
        return getDevicesOfType(SimpleLight.class)
                .stream()
                .filter(light -> !homeSystemProperties.getNotLights().contains(light.getName()))
                .anyMatch(SimpleLight::isCurrentStateIsOn);
    }
}
