package ch.akop.homesystem.services.impl;

import ch.akop.homesystem.models.devices.Device;
import ch.akop.homesystem.models.devices.actor.Light;
import ch.akop.homesystem.services.DeviceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;


@RequiredArgsConstructor
@Service
@Slf4j
public class DeviceServiceImpl implements DeviceService {

    private final Map<String, Device<?>> devices = new HashMap<>();


    public <T extends Device<?>> T getDevice(final String id, final Class<T> clazz) {
        return clazz.cast(this.devices.get(id));
    }

    @Override
    public void registerDevice(final Device<?> device) {
        this.devices.put(device.getId(), device);
    }

    @Override
    public Collection<Device<?>> getAllDevices() {
        return this.devices.values();
    }

    @Override
    public Collection<Light> getAllLights() {
        return this.devices.values().stream()
                .filter(Light.class::isInstance)
                .map(Light.class::cast)
                .collect(Collectors.toSet());
    }

    @Override
    public <T> Collection<T> getDevicesOfType(final Class<T> clazz) {
        return this.devices.values().stream()
                .filter(clazz::isInstance)
                .map(clazz::cast)
                .collect(Collectors.toSet());
    }


}
