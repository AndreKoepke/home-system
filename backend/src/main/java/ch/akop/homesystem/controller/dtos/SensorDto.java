package ch.akop.homesystem.controller.dtos;

import ch.akop.homesystem.models.devices.sensor.MotionSensor;
import ch.akop.homesystem.models.devices.sensor.Sensor;
import ch.akop.homesystem.persistence.model.animation.Animation;
import ch.akop.homesystem.persistence.model.config.MotionSensorConfig;
import jakarta.annotation.Nullable;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
public class SensorDto implements Identable {

  private String id;
  private String name;

  // some sensors change that value very often
  // it causes a lot of websocket messages
  @EqualsAndHashCode.Exclude
  private ZonedDateTime lastUpdate;

  private LocalDateTime presenceChangedAt;
  private boolean reachable;
  private boolean presence;

  @Nullable
  private ConfigDto config;

  public SensorDto appendConfig(MotionSensorConfig config) {
    return setConfig(ConfigDto.from(config));
  }

  @Data
  @Builder
  public static class ConfigDto {

    private String name;
    private Collection<String> lights;
    private Collection<String> lightsAtNight;
    private Duration keepMovingFor;
    @Nullable
    private Integer onlyTurnOnWhenDarkerAs;
    @Nullable
    private Integer selfLightNoise;
    private boolean turnLightOnWhenMovement;
    @Nullable
    private LocalTime notBefore;
    @Nullable
    private UUID animationId;
    @Nullable
    private UUID animationAtNightId;

    public static ConfigDto from(MotionSensorConfig config) {
      return ConfigDto.builder()
          .name(config.getName())
          .lights(config.getLights())
          .lightsAtNight(config.getLightsAtNight())
          .keepMovingFor(config.getKeepMovingFor())
          .onlyTurnOnWhenDarkerAs(config.getOnlyTurnOnWhenDarkerAs())
          .selfLightNoise(config.getSelfLightNoise())
          .turnLightOnWhenMovement(config.isTurnLightOnWhenMovement())
          .notBefore(config.getNotBefore())
          .animationId(Optional.ofNullable(config.getAnimation()).map(Animation::getId).orElse(null))
          .animationAtNightId(Optional.ofNullable(config.getAnimationNight()).map(Animation::getId).orElse(null))
          .build();
    }
  }


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
