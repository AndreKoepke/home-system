package ch.akop.homesystem.models.events;

import lombok.Data;

@Data
public class CubeEvent {

  private final String cubeName;
  private final CubeEventType eventType;
}
