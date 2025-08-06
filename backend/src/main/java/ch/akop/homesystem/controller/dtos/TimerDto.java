package ch.akop.homesystem.controller.dtos;

import ch.akop.homesystem.persistence.model.config.TimerConfig;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import lombok.Data;
import org.jetbrains.annotations.Nullable;

@Data
public class TimerDto {

  private UUID id;
  private String name;
  private List<String> lights;

  @Nullable
  private LocalTime turnOffAt;

  @Nullable
  private LocalTime turnOnAt;

  public static TimerDto from(TimerConfig config) {
    return new TimerDto()
        .setId(config.getId())
        .setName(config.getName())
        .setLights(config.getDevices())
        .setTurnOffAt(config.getTurnOffAt())
        .setTurnOnAt(config.getTurnOnAt());
  }

  public TimerConfig toObject() {
    return new TimerConfig()
        .setId(getId())
        .setName(getName())
        .setDevices(getLights())
        .setTurnOffAt(getTurnOffAt())
        .setTurnOnAt(getTurnOnAt());
  }
}
