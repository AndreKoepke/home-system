package ch.akop.homesystem.controller.dtos;

import ch.akop.homesystem.models.devices.sensor.MotionSensor;
import ch.akop.homesystem.models.devices.sensor.Sensor;
import lombok.Data;

@Data
public class SensorDto {

  private String id;
  private String name;
  private boolean reachable;
  private boolean presence;


  public static SensorDto from(Sensor<?> sensor) {
    return new SensorDto()
        .setId(sensor.getId())
        .setName(sensor.getName())
        .setReachable(sensor.isReachable());
  }

  public static SensorDto from(MotionSensor motionSensor) {
    return from(motionSensor)
        .setPresence(motionSensor.getIsMoving$().blockingFirst());
  }
}
