package ch.akop.homesystem.models.devices.actor;

import ch.akop.homesystem.models.devices.Device;

/**
 * Wrapper, because IDs are not unique. Instead of a field, it's marked with the super-class.
 */
public abstract class Actor<T> extends Device<Actor<T>> {
}
