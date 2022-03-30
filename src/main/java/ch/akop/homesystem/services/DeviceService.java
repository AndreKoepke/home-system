package ch.akop.homesystem.services;

import ch.akop.homesystem.models.devices.Device;

import java.util.Collection;

public interface DeviceService {

    <T extends Device<?>> T getDevice(String id, Class<T> clazz);

    <T extends Device<?>> void registerDevice(T device);

    Collection<Device<?>> getAllDevices();

    <T> Collection<T> getDevicesOfType(Class<T> clazz);

}
