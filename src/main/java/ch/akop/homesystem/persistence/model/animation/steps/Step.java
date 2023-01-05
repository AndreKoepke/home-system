package ch.akop.homesystem.persistence.model.animation.steps;

import ch.akop.homesystem.services.DeviceService;

public interface Step {
    void play(DeviceService deviceService);

    Integer getSortOrder();
}
