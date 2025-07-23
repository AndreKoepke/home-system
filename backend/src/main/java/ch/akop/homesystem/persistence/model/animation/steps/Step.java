package ch.akop.homesystem.persistence.model.animation.steps;

import ch.akop.homesystem.services.impl.DeviceService;
import java.util.UUID;

public interface Step {

  void play(DeviceService deviceService);

  Integer getSortOrder();

  UUID getId();

  String getNameOfLight();

  String getActionDescription();
}
