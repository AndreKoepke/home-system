package ch.akop.homesystem.models.devices.sensor;

import ch.akop.homesystem.models.devices.Device;

/**
 * Wrapper, because IDs are not unique. Instead of a field, it's marked with the super-class.
 */
public abstract class Sensor<T> extends Device<Sensor<T>> {
}
