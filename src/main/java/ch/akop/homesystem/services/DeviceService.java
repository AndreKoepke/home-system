package ch.akop.homesystem.services;

import ch.akop.homesystem.models.devices.Device;

import java.util.Collection;
import java.util.Optional;

public interface DeviceService {

    <T extends Device<?>> Optional<T> findDeviceByName(String name, Class<T> clazz);

    <T extends Device<?>> Optional<T> findDeviceById(String name, Class<T> clazz);

    <T extends Device<?>> void registerDevice(T device);

    Collection<Device<?>> getAllDevices();

    <T> Collection<T> getDevicesOfType(Class<T> clazz);

    void turnAllLightsOff();

    boolean isAnyLightOn();

    void activeSceneForAllGroups(String sceneName);

}
