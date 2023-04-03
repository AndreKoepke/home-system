package ch.akop.homesystem.models.events;

import ch.akop.homesystem.models.devices.sensor.CloseContactState;
import lombok.Data;

@Data
public class CloseContactEvent {

  private final String name;
  private final CloseContactState newState;
}
