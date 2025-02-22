package ch.akop.homesystem.controller.dtos;

import ch.akop.homesystem.models.CompassDirection;
import ch.akop.homesystem.models.devices.actor.RollerShutter;
import ch.akop.homesystem.persistence.model.config.RollerShutterConfig;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import lombok.Data;
import org.springframework.lang.Nullable;

@Data
public class RollerShutterDto {

  private String id;
  private String name;
  private boolean reachable;

  @Min(0)
  @Max(100)
  private Integer currentLift;

  /**
   * Tilt angle
   */
  @Min(0)
  @Max(100)
  private Integer currentTilt;

  private boolean isOpen;

  private ConfigDto config;

  public static RollerShutterDto from(RollerShutter rollerShutter) {
    return new RollerShutterDto()
        .setId(rollerShutter.getId())
        .setName(rollerShutter.getName())
        .setReachable(rollerShutter.isReachable())
        .setCurrentLift(rollerShutter.getCurrentLift())
        .setCurrentTilt(rollerShutter.getCurrentTilt())
        .setOpen(rollerShutter.isOpen())
        .setConfig(ConfigDto.from(rollerShutter.getConfig()));
  }

  @Data
  public static class ConfigDto {

    private List<CompassDirection> compassDirection;

    @Nullable
    private LocalTime closeAt;

    @Nullable
    private LocalTime openAt;

    @Nullable
    private LocalDateTime noAutomaticsUntil;

    @Nullable
    private Boolean closeWithInterrupt;

    public static ConfigDto from(RollerShutterConfig config) {
      return new ConfigDto()
          .setCompassDirection(config.getCompassDirection())
          .setCloseAt(config.getCloseAt())
          .setOpenAt(config.getOpenAt())
          .setNoAutomaticsUntil(config.getNoAutomaticsUntil())
          .setCloseWithInterrupt(config.getCloseWithInterrupt());
    }
  }
}
