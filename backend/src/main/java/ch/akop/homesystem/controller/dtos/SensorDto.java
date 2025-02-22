package ch.akop.homesystem.controller.dtos;

import ch.akop.homesystem.models.devices.sensor.MotionSensor;
import ch.akop.homesystem.models.devices.sensor.Sensor;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import lombok.Data;

@Data
public class SensorDto {

  private String id;
  private String name;
  private ZonedDateTime lastUpdate;
  private LocalDateTime presenceChangedAt;
  private boolean reachable;
  private boolean presence;


  public static SensorDto from(Sensor<?> sensor) {
    return new SensorDto()
        .setId(sensor.getId())
        .setName(sensor.getName())
        .setReachable(sensor.isReachable())
        .setLastUpdate(sensor.getLastUpdated());
  }

  public static SensorDto from(MotionSensor motionSensor) {
    return from((Sensor<?>) motionSensor)
        .setPresenceChangedAt(motionSensor.getMovingChangedAt())
        .setPresence(motionSensor.isMoving());
  }
}
