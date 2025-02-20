package ch.akop.homesystem.controller.dtos;

import ch.akop.homesystem.models.color.Color;
import ch.akop.homesystem.models.devices.actor.Actor;
import ch.akop.homesystem.models.devices.actor.ColoredLight;
import ch.akop.homesystem.models.devices.actor.DimmableLight;
import ch.akop.homesystem.models.devices.actor.SimpleLight;
import java.time.ZonedDateTime;
import lombok.Data;

@Data
public class ActorDto {

  private String id;
  private String name;
  private int brightness;
  private Color color;
  private boolean on;
  private boolean reachable;
  private ZonedDateTime lastUpdated;


  public static ActorDto from(Actor<?> light) {
    return new ActorDto()
        .setId(light.getId())
        .setName(light.getName())
        .setReachable(light.isReachable())
        .setLastUpdated(light.getLastUpdated());
  }

  public static ActorDto from(SimpleLight light) {
    return from((Actor<?>) light)
        .setOn(light.isCurrentStateIsOn());
  }

  public static ActorDto from(DimmableLight light) {
    return from((SimpleLight) light)
        .setBrightness((int) (light.getCurrentBrightness() / 255f * 100));
  }

  public static ActorDto from(ColoredLight light) {
    return from((DimmableLight) light)
        .setColor(light.getCurrentColor());
  }
}
