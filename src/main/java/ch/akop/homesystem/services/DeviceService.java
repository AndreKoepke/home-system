package ch.akop.homesystem.services;

import ch.akop.homesystem.models.devices.Device;
import ch.akop.homesystem.models.devices.actor.Light;

import java.util.Collection;

public interface DeviceService {

    <T extends Device<?>> T getDevice(String id, Class<T> clazz);

    void registerDevice(Device<?> device);

    Collection<Device<?>> getAllDevices();

    Collection<Light> getAllLights();

    <T> Collection<T> getDevicesOfType(Class<T> clazz);

}
