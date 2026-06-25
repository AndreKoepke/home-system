package ch.akop.homesystem.controller.dtos;

import ch.akop.homesystem.models.devices.sensor.LightLevel;
import ch.akop.homesystem.models.devices.sensor.MotionSensor;
import ch.akop.homesystem.models.devices.sensor.Sensor;
import ch.akop.homesystem.persistence.model.animation.Animation;
import ch.akop.homesystem.persistence.model.config.MotionSensorConfig;
import io.reactivex.rxjava3.subjects.ReplaySubject;
import jakarta.annotation.Nullable;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
public class MotionSensorDto implements Identable {

  private String id;
  private String name;

  // some sensors change that value very often
  // it causes a lot of websocket messages
  @EqualsAndHashCode.Exclude
  private ZonedDateTime lastUpdate;

  private LocalDateTime presenceChangedAt;
  private boolean reachable;
  private boolean presence;
  private boolean dark;
  private Integer brightness;

  @Nullable
  private ConfigDto config;

  public MotionSensorDto appendConfig(MotionSensorConfig config) {
    return setConfig(ConfigDto.from(config));
  }

  @Data
  @Builder
  public static class ConfigDto {

    private String name;
    private Set<String> lights;
    private Set<String> lightsAtNight;
    private Duration keepMovingFor;
    @Nullable
    private Integer onlyTurnOnWhenDarkerAs;
    @Nullable
    private Integer selfLightNoise;
    private boolean turnLightOnWhenMovement;
    @Nullable
    String turnOnWhenRollerShutterIsClosed;
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
          .turnOnWhenRollerShutterIsClosed(config.getTurnOnWhenRollerShutterIsClosed())
          .selfLightNoise(config.getSelfLightNoise())
          .turnLightOnWhenMovement(config.isTurnLightOnWhenMovement())
          .notBefore(config.getNotBefore())
          .animationId(Optional.ofNullable(config.getAnimation()).map(Animation::getId).orElse(null))
          .animationAtNightId(Optional.ofNullable(config.getAnimationNight()).map(Animation::getId).orElse(null))
          .build();
    }
  }

  public static MotionSensorDto from(Sensor<?> sensor) {
    return new MotionSensorDto()
        .setId(sensor.getId())
        .setName(sensor.getName())
        .setReachable(sensor.isReachable())
        .setLastUpdate(sensor.getLastUpdated());
  }

  public static MotionSensorDto from(MotionSensor motionSensor) {
    return from((Sensor<?>) motionSensor)
        .setPresenceChangedAt(motionSensor.getMovingChangedAt())
        .setPresence(motionSensor.isMoving())
        .setDark(motionSensor.isDark())
        .setBrightness(Optional.ofNullable(motionSensor.getLightLevel())
            .map(LightLevel::getLux$)
            .map(ReplaySubject::getValue)
            .orElse(null));
  }
}
