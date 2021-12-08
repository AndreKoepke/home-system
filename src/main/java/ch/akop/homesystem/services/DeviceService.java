package ch.akop.homesystem.services;

import ch.akop.homesystem.models.devices.Device;

import java.util.Collection;

public interface DeviceService {

     <T extends Device<?>> T getDevice(String id, Class<T> clazz);
     void registerDevice(Device<?> device);
     Collection<Device<?>> getAllDevices();

}
